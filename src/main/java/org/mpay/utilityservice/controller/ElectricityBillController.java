package org.mpay.utilityservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mpay.utilityservice.service.GarageS3Service;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/electricity-bills")
@RequiredArgsConstructor
@Slf4j
public class ElectricityBillController {

    private final JobLauncher jobLauncher;
    private final Job importElectricityBillJob;
    private final GarageS3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Receiving upload request for: {}", file.getOriginalFilename());

            // 1. Persist to Garage (S3) for audit and durability
            String s3Key = s3Service.uploadToGarage(file);

            // 2. Localize the file for Apache POI (Random Access requirement)
            String localPath = s3Service.downloadFromGarage(s3Key);

            // 3. Construct Job Parameters
            JobParameters params = new JobParametersBuilder()
                    .addString("s3Key", s3Key)           // Keep for Audit Trail
                    .addString("localFilePath", localPath) // Used by the Reader
                    .addString("originalFileName", file.getOriginalFilename())
                    .addLong("time", System.currentTimeMillis()) // Ensures unique job instance
                    .toJobParameters();

            // 4. Fire and Forget the Batch Job
            jobLauncher.run(importElectricityBillJob, params);

            return ResponseEntity.ok("File uploaded successfully. Processing started. Reference: " + s3Key);

        } catch (Exception e) {
            log.error("Failed to start electricity bill import process", e);
            return ResponseEntity.internalServerError().body("Import failed: " + e.getMessage());
        }
    }
}