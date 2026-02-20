package org.mpay.utilityservice.dto;

public record BillValidationResult(
        ElectricityBillRaw rawData,
        boolean isValid,
        String errorMsg
) {}