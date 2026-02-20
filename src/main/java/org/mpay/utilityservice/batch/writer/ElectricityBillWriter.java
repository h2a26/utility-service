package org.mpay.utilityservice.batch.writer;

import lombok.RequiredArgsConstructor;
import org.mpay.utilityservice.dto.BillValidationResult;
import org.mpay.utilityservice.dto.ElectricityBillRaw;
import org.mpay.utilityservice.service.ElectricityBillService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@StepScope // Added to access the current jobId from the context
@RequiredArgsConstructor
public class ElectricityBillWriter implements ItemWriter<BillValidationResult> {

    private final ElectricityBillService billService;

    // Retrieve the Job ID from the Spring Batch context
    @Value("#{stepExecution.jobExecution.id}")
    private Long jobId;

    @Override
    public void write(Chunk<? extends BillValidationResult> chunk) throws Exception {

        // 1. Partition the chunk into two lists based on the isValid boolean
        Map<Boolean, List<BillValidationResult>> results = chunk.getItems().stream()
                .collect(Collectors.partitioningBy(BillValidationResult::isValid));

        // 2. Handle the 'Success' path
        List<ElectricityBillRaw> validItems = results.get(true).stream()
                .map(BillValidationResult::rawData)
                .collect(Collectors.toList());

        if (!validItems.isEmpty()) {
            billService.saveValidBills(validItems);
        }

        // 3. Handle the 'Failure' path (Crucial for your report!)
        List<BillValidationResult> failedItems = results.get(false);

        if (!failedItems.isEmpty()) {
            // We pass the jobId so we know which upload these failures belong to
            billService.saveFailedBills(failedItems, jobId);
        }
    }
}