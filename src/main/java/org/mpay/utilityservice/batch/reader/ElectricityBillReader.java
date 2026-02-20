package org.mpay.utilityservice.batch.reader;

import lombok.extern.slf4j.Slf4j;
import org.mpay.utilityservice.dto.ElectricityBillRaw;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.extensions.excel.RowMapper;
import org.springframework.batch.extensions.excel.poi.PoiItemReader;
import org.springframework.batch.extensions.excel.support.rowset.RowSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ElectricityBillReader {

    /**
     * The reader is StepScoped so it can access jobParameters at runtime.
     * It reads from the local path provided by the Controller/S3 download.
     */
    @Bean
    @StepScope
    public PoiItemReader<ElectricityBillRaw> excelBillReader(
            @Value("#{jobParameters['localFilePath']}") String path) {

        log.info("Initializing Excel Reader for file: {}", path);

        PoiItemReader<ElectricityBillRaw> reader = new PoiItemReader<>();

        reader.setName("electricityBillExcelReader");

        // 1. Set the local file resource
        reader.setResource(new FileSystemResource(path));

        // 2. Skip the header row
        reader.setLinesToSkip(1);

        // 3. Map Excel Rows to our Raw DTO
        reader.setRowMapper(billRowMapper());

        return reader;
    }

    private RowMapper<ElectricityBillRaw> billRowMapper() {
        return (RowSet rowSet) -> {
            String[] row = rowSet.getCurrentRow();
            return new ElectricityBillRaw(
                    safeGet(row, 0), safeGet(row, 1), safeGet(row, 2), safeGet(row, 3),
                    safeGet(row, 4), safeGet(row, 5), safeGet(row, 6), safeGet(row, 7),
                    safeGet(row, 8), safeGet(row, 9), safeGet(row, 10), safeGet(row, 11),
                    safeGet(row, 12), safeGet(row, 13), safeGet(row, 14), safeGet(row, 15),
                    safeGet(row, 16), safeGet(row, 17), safeGet(row, 18), safeGet(row, 19),
                    safeGet(row, 20), safeGet(row, 21), safeGet(row, 22), safeGet(row, 23),
                    safeGet(row, 24), safeGet(row, 25), safeGet(row, 26), safeGet(row, 27)
            );
        };
    }

    private String safeGet(String[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return "";
        }
        return row[index].trim();
    }
}