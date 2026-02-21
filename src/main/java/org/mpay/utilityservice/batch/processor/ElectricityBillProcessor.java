// src/main/java/org/mpay/utilityservice/batch/processor/ElectricityBillProcessor.java
package org.mpay.utilityservice.batch.processor;

import org.mpay.utilityservice.dto.BillValidationResult;
import org.mpay.utilityservice.dto.ElectricityBillRaw;
import org.mpay.utilityservice.entity.ElectricityBill;
import org.mpay.utilityservice.util.BillMapper;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ElectricityBillProcessor implements ItemProcessor<ElectricityBillRaw, BillValidationResult> {

    @Override
    public BillValidationResult process(ElectricityBillRaw item) {
        List<String> errors = new ArrayList<>();

        // 1. Validate Mandatory 'NOT NULL' Strings before mapping
        validateRequired(item.ledgerNo(), "Ledger No", errors);
        validateRequired(item.consumerNo(), "Consumer No", errors);
        validateRequired(item.consumerName(), "Consumer Name", errors);

        // 2. Delegate Construction & Detailed Parsing to the Mapper
        ElectricityBill entity = BillMapper.mapToEntity(item, errors);

        // 3. Check if any errors occurred during mandatory checks or mapping
        if (!errors.isEmpty()) {
            return new BillValidationResult(item, null, false, String.join(" | ", errors));
        }

        return new BillValidationResult(item, entity, true, null);
    }

    private void validateRequired(String val, String field, List<String> errors) {
        if (val == null || val.isBlank() || val.equalsIgnoreCase("null")) {
            errors.add(field + " is mandatory");
        }
    }
}