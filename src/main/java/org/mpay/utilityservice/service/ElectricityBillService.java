// src/main/java/org/mpay/utilityservice/service/ElectricityBillService.java
package org.mpay.utilityservice.service;

import lombok.RequiredArgsConstructor;
import org.mpay.utilityservice.dto.BillValidationResult;
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

    /**
     * Now accepts pre-constructed Entities.
     * This ensures the Service doesn't fail due to mapping errors.
     */
    @Transactional
    public void saveEntities(List<ElectricityBill> entities) {
        if (!entities.isEmpty()) {
            repository.saveAll(entities);
        }
    }

    @Transactional
    public void saveFailedBills(List<BillValidationResult> failedResults, Long jobId) {
        List<FailedElectricityBill> failedEntities = failedResults.stream()
                .map(result -> BillMapper.mapToFailedEntity(result, jobId))
                .collect(Collectors.toList());

        if (!failedEntities.isEmpty()) {
            failedRepository.saveAll(failedEntities);
        }
    }

    public List<FailedElectricityBill> getFailedBillsForReport() {
        return failedRepository.findTop1000ByOrderByCreatedDateDesc();
    }
}