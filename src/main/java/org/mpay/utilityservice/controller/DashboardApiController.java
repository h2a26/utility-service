package org.mpay.utilityservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mpay.utilityservice.dto.JobExecutionInfo;
import org.mpay.utilityservice.entity.FailedElectricityBill;
import org.mpay.utilityservice.service.ElectricityBillService;
import org.mpay.utilityservice.service.GarageS3Service;
import org.mpay.utilityservice.service.JobMonitoringService;
import org.mpay.utilityservice.util.ExcelExportUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/dashboard/api")
@RequiredArgsConstructor
@Slf4j
public class DashboardApiController {

    private final JobLauncher jobLauncher;
    private final Job importElectricityBillJob;
    private final GarageS3Service s3Service;
    private final ElectricityBillService billService;
    private final JobMonitoringService jobMonitoringService;

    @PostMapping(value = "/upload", produces = MediaType.TEXT_HTML_VALUE)
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
        try {
            log.info("Receiving upload request for: {}", file.getOriginalFilename());

            String s3Key = s3Service.uploadToGarage(file);
            String localPath = s3Service.downloadFromGarage(s3Key);

            JobParameters params = new JobParametersBuilder()
                    .addString("s3Key", s3Key)
                    .addString("localFilePath", localPath)
                    .addString("originalFileName", file.getOriginalFilename())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(importElectricityBillJob, params);

            model.addAttribute("success", true);
            model.addAttribute("message", "File uploaded successfully. Processing started.");
            model.addAttribute("jobId", jobExecution.getId());
            model.addAttribute("fileName", file.getOriginalFilename());

            return "fragments/upload-result :: success";

        } catch (Exception e) {
            log.error("Failed to start electricity bill import process", e);
            model.addAttribute("success", false);
            model.addAttribute("error", e.getMessage());
            return "fragments/upload-result :: error";
        }
    }

    @GetMapping(value = "/jobs/recent", produces = MediaType.TEXT_HTML_VALUE)
    public String getRecentJobs(Model model) {
        List<JobExecutionInfo> recentJobs = jobMonitoringService.getRecentJobExecutions(10);
        model.addAttribute("jobs", recentJobs);
        return "fragments/job-table :: job-rows";
    }


    @GetMapping("/jobs/export-failed")
    public ResponseEntity<byte[]> exportFailedReport() {
        try {
            List<FailedElectricityBill> failedBills = billService.getFailedBillsForReport();
            byte[] excelContent = ExcelExportUtils.generateFailedBillReport(failedBills);

            String fileName = "failed_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelContent);

        } catch (Exception e) {
            log.error("Export failed report error", e);
            return ResponseEntity.internalServerError().build();
        }
    }

}