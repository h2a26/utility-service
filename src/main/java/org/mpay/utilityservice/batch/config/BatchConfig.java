package org.mpay.utilityservice.batch.config;

import org.mpay.utilityservice.batch.listener.FileCleanupListener;
import org.mpay.utilityservice.batch.processor.ElectricityBillProcessor;
import org.mpay.utilityservice.batch.writer.ElectricityBillWriter;
import org.mpay.utilityservice.dto.BillValidationResult;
import org.mpay.utilityservice.dto.ElectricityBillRaw;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.extensions.excel.poi.PoiItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    @Value("${spring-batch.chunk-size:100}")
    private int chunkSize;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final FileCleanupListener fileCleanupListener;

    public BatchConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager, FileCleanupListener fileCleanupListener) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.fileCleanupListener = fileCleanupListener;
    }

    @Bean
    public Job importElectricityBillJob(Step importBillStep) {
        return new JobBuilder("importElectricityBillJob", jobRepository)
                .listener(fileCleanupListener)
                .start(importBillStep)
                .build();
    }

    @Bean
    public Step importBillStep(
            PoiItemReader<ElectricityBillRaw> reader,
            ElectricityBillProcessor processor,
            ElectricityBillWriter writer) {

        return new StepBuilder("importBillStep", jobRepository)
                .<ElectricityBillRaw, BillValidationResult>chunk(chunkSize, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}