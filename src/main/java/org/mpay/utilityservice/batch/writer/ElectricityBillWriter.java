package org.mpay.utilityservice.batch.writer;

import lombok.RequiredArgsConstructor;
import org.mpay.utilityservice.dto.BillValidationResult;
import org.mpay.utilityservice.entity.ElectricityBill;
import org.mpay.utilityservice.service.ElectricityBillService;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
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
    public void write(Chunk<? extends BillValidationResult> chunk) throws Exception {
        // 1. Partition the results based on validation status
        Map<Boolean, List<BillValidationResult>> results = chunk.getItems().stream()
                .collect(Collectors.partitioningBy(BillValidationResult::isValid));

        List<ElectricityBill> validEntities = results.get(true).stream()
                .map(BillValidationResult::validatedEntity)
                .toList();

        List<BillValidationResult> failedItems = results.get(false);

        // 2. Persistent Save to respective tables
        if (!validEntities.isEmpty()) {
            billService.saveEntities(validEntities);
        }

        if (!failedItems.isEmpty()) {
            billService.saveFailedBills(failedItems, jobId);
        }

        // 3. Update CUSTOM Logic Counters in the ExecutionContext
        // This persists the count even if the job is interrupted or running in chunks
        ExecutionContext stepContext = stepExecution.getExecutionContext();

        long currentSuccess = stepContext.getLong("CUSTOM_SUCCESS_COUNT", 0L);
        long currentFailed = stepContext.getLong("CUSTOM_FAILED_COUNT", 0L);

        stepContext.putLong("CUSTOM_SUCCESS_COUNT", currentSuccess + validEntities.size());
        stepContext.putLong("CUSTOM_FAILED_COUNT", currentFailed + failedItems.size());
    }
}