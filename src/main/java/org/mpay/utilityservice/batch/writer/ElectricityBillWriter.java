package org.mpay.utilityservice.batch.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mpay.utilityservice.dto.BillValidationResult;
import org.mpay.utilityservice.entity.FailedElectricityBill;
import org.mpay.utilityservice.service.ElectricityBillService;
import org.mpay.utilityservice.util.BillMapper;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class ElectricityBillWriter implements ItemWriter<BillValidationResult> {

    private final ElectricityBillService billService;
    private StepExecution stepExecution;

    @BeforeStep
    public void saveStepExecution(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Value("#{stepExecution.jobExecution.id}")
    private Long jobId;

    @Override
    public void write(Chunk<? extends BillValidationResult> chunk) {
        // Use thread-safe collection and counters for parallel processing
        ConcurrentLinkedQueue<BillValidationResult> failureQueue = new ConcurrentLinkedQueue<>();
        LongAdder successCounter = new LongAdder();

        // Use parallelStream for concurrent DB inserts
        chunk.getItems().parallelStream().forEach(result -> {
            if (result.isValid()) {
                try {
                    billService.saveSingleEntity(result.validatedEntity());
                    successCounter.increment();
                } catch (Exception e) {
                    log.error("Job {} | Row {}: DB Save Error -> {}", jobId, result.rawData().rowNumber(), e.getMessage());
                    failureQueue.add(new BillValidationResult(result.rawData(), null, false, "DB Error: " + e.getMessage()));
                }
            } else {
                failureQueue.add(result);
            }
        });

        // Batch save failures (usually a smaller set, keeps it efficient)
        if (!failureQueue.isEmpty()) {
            List<FailedElectricityBill> failedEntities = failureQueue.stream()
                    .map(r -> BillMapper.mapToFailedEntity(r, jobId))
                    .toList();
            billService.saveFailedBills(failedEntities);
        }

        updateStepContextAndLog(successCounter.sum(), failureQueue.size());
    }

    private synchronized void updateStepContextAndLog(long successDelta, long failedDelta) {
        var ec = stepExecution.getExecutionContext();

        long totalSuccess = ec.getLong("CUSTOM_SUCCESS_COUNT", 0L) + successDelta;
        long totalFailed = ec.getLong("CUSTOM_FAILED_COUNT", 0L) + failedDelta;

        ec.putLong("CUSTOM_SUCCESS_COUNT", totalSuccess);
        ec.putLong("CUSTOM_FAILED_COUNT", totalFailed);

        log.info(">>> Job ID: {} | Total Read: {} | Chunk Success: {} | Chunk Failed: {} | Cumulative (S: {}, F: {})",
                jobId, stepExecution.getReadCount(), successDelta, failedDelta, totalSuccess, totalFailed);
    }
}