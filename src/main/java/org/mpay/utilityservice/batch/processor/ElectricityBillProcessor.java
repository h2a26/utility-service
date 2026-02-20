package org.mpay.utilityservice.batch.processor;

import org.mpay.utilityservice.dto.ElectricityBillRaw;
import org.mpay.utilityservice.dto.BillValidationResult;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class ElectricityBillProcessor implements ItemProcessor<ElectricityBillRaw, BillValidationResult> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public BillValidationResult process(ElectricityBillRaw item) {
        List<String> errors = new ArrayList<>();

        // 1. Validate Date (YYYYMMDD)
        if (item.billDueDate() == null || !isValidDate(item.billDueDate())) {
            errors.add("Invalid bill_due_date [" + item.billDueDate() + "]. Expected YYYYMMDD.");
        }

        // 2. Validate Required Numeric Fields
        validateBigDecimal(item.billAmount(), "bill_amount", errors);
        validateBigDecimal(item.totalBillAmount(), "total_bill_amount", errors);
        validateBigDecimal(item.grandTotalAmount(), "grand_total_amount", errors);

        // Validate Integers
        validateInteger(item.usedUnit(), "used_unit", errors);

        if (!errors.isEmpty()) {
            return new BillValidationResult(item, false, String.join(" | ", errors));
        }

        return new BillValidationResult(item, true, null);
    }

    private boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr, DATE_FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateBigDecimal(String value, String fieldName, List<String> errors) {
        try {
            if (value == null || value.isBlank()) return; // Optional fields check logic
            new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            errors.add(fieldName + " must be a valid number.");
        }
    }

    private void validateInteger(String value, String fieldName, List<String> errors) {
        try {
            if (value == null || value.isBlank()) return;
            Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            errors.add(fieldName + " must be an integer.");
        }
    }
}