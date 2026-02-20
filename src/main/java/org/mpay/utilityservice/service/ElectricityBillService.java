package org.mpay.utilityservice.service;

import lombok.RequiredArgsConstructor;
import org.mpay.utilityservice.dto.ElectricityBillRaw;
import org.mpay.utilityservice.entity.ElectricityBill;
import org.mpay.utilityservice.repository.ElectricityBillRepository;
import org.mpay.utilityservice.util.BillMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ElectricityBillService {

    private final ElectricityBillRepository repository;

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
}