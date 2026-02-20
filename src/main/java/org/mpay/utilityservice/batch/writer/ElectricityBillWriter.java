package org.mpay.utilityservice.batch.writer;

import lombok.RequiredArgsConstructor;
import org.mpay.utilityservice.dto.BillValidationResult;
import org.mpay.utilityservice.dto.ElectricityBillRaw;
import org.mpay.utilityservice.service.ElectricityBillService;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ElectricityBillWriter implements ItemWriter<BillValidationResult> {

    private final ElectricityBillService billService;

    /**
     * Receives the processed and validated chunk from the Batch Step.
     * Filters for valid records and sends them to the service for persistence.
     */
    @Override
    public void write(Chunk<? extends BillValidationResult> chunk) throws Exception {

        // Extract only the raw data from results marked as 'valid'
        List<ElectricityBillRaw> validItems = chunk.getItems().stream()
                .filter(BillValidationResult::isValid)
                .map(BillValidationResult::rawData)
                .collect(Collectors.toList());

        // Hand over the valid list to the Service for mapping and DB saving
        if (!validItems.isEmpty()) {
            billService.saveValidBills(validItems);
        }

        // Note: The 'isValid == false' records are ignored here for now
        // as per the focus on the Happy Flow.
    }
}