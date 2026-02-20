package org.mpay.utilityservice.service;

import lombok.RequiredArgsConstructor;
import org.mpay.utilityservice.dto.BillValidationResult;
import org.mpay.utilityservice.dto.ElectricityBillRaw;
import org.mpay.utilityservice.entity.ElectricityBill;
import org.mpay.utilityservice.entity.FailedElectricityBill;
import org.mpay.utilityservice.repository.ElectricityBillRepository;
import org.mpay.utilityservice.repository.FailedElectricityBillRepository;
import org.mpay.utilityservice.util.BillMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ElectricityBillService {

    private final ElectricityBillRepository repository;
    private final FailedElectricityBillRepository failedRepository;

    @Transactional
    public void saveValidBills(List<ElectricityBillRaw> rawBills) {
        // Transform the list of Raw DTOs (Strings) into JPA Entities (Typed)
        List<ElectricityBill> entities = rawBills.stream()
                .map(BillMapper::mapToEntity)
                .collect(Collectors.toList());

        // Save the collection to the prepost.electricity_bill table
        if (!entities.isEmpty()) {
            repository.saveAll(entities);
        }
    }

    public List<FailedElectricityBill> getFailedBillsForReport() {
        // Fetching the most recent failed records for the report
        return failedRepository.findTop1000ByOrderByCreatedDateDesc();
    }

    @Transactional
    public void saveFailedBills(List<BillValidationResult> failedResults, Long jobId) {
        // Transform the list of Validation Results into Failed Entities using the Mapper
        List<FailedElectricityBill> failedEntities = failedResults.stream()
                .map(result -> BillMapper.mapToFailedEntity(result, jobId))
                .collect(Collectors.toList());

        if (!failedEntities.isEmpty()) {
            failedRepository.saveAll(failedEntities);
        }
    }
}