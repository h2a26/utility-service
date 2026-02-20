package org.mpay.utilityservice.util;

import org.mpay.utilityservice.dto.ElectricityBillRaw;
import org.mpay.utilityservice.entity.ElectricityBill;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BillMapper {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static ElectricityBill mapToEntity(ElectricityBillRaw raw) {
        LocalDate billDueDate = LocalDate.parse(raw.billDueDate(), DATE_FORMATTER);

        return ElectricityBill.builder()
                .ledgerNo(raw.ledgerNo())
                .consumerNo(raw.consumerNo())
                .meterNo(raw.meterNo())
                .consumerName(raw.consumerName())
                .address(raw.address())
                .billCode(raw.billCode())
                .billDueDate(billDueDate)
                .billMonth(billDueDate.getMonthValue())
                .billYear(billDueDate.getYear())
                .usedUnit(Integer.parseInt(raw.usedUnit()))
                .billAmount(parseDecimal(raw.billAmount()))
                .serviceCharges(parseDecimal(raw.serviceCharges()))
                .horsePowerCharges(parseDecimal(raw.horsePowerCharges()))
                .discount(parseDecimal(raw.discount()))
                .lastBalance(parseDecimal(raw.lastBalance()))
                .totalBillAmount(parseDecimal(raw.totalBillAmount()))
                .debtBalance(parseDecimal(raw.debtBalance()))
                .installationFee(parseDecimal(raw.installationFee()))
                .grandTotalAmount(parseDecimal(raw.grandTotalAmount()))
                .isActiveConsumer(true)
                .township(raw.township())
                .terrifCode(raw.terrifCode())
                .readingDate(parseDateTime(raw.readingDate()))
                .previousUnit(parseInteger(raw.previousUnit()))
                .currentUnit(parseInteger(raw.currentUnit()))
                .houseNo(raw.houseNo())
                .road(raw.road())
                .quarter(raw.quarter())
                .area(raw.area())
                .billNo(raw.billNo())
                .deposit(parseDecimal(raw.deposit()))
                .build();
    }

    private static BigDecimal parseDecimal(String val) {
        if (val == null || val.isBlank()) return BigDecimal.ZERO;
        return new BigDecimal(val.trim());
    }

    private static Integer parseInteger(String val) {
        if (val == null || val.isBlank()) return null;
        return Integer.parseInt(val.trim());
    }

    private static LocalDateTime parseDateTime(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return LocalDateTime.parse(val.trim(), DATE_TIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}