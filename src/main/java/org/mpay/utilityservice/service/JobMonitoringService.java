package org.mpay.utilityservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mpay.utilityservice.dto.JobExecutionInfo;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobMonitoringService {

    private final JobExplorer jobExplorer;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public List<JobExecutionInfo> getRecentJobExecutions(int limit) {
        try {
            return jobExplorer.findJobInstancesByJobName("importElectricityBillJob", 0, limit)
                    .stream()
                    .flatMap(jobInstance -> jobExplorer.getJobExecutions(jobInstance).stream())
                    .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                    .limit(limit)
                    .map(this::mapToJobExecutionInfo)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching recent job executions", e);
            return List.of();
        }
    }

    public JobExecutionInfo getJobExecutionInfo(Long jobExecutionId) {
        try {
            JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
            if (jobExecution != null) {
                return mapToJobExecutionInfo(jobExecution);
            }
        } catch (Exception e) {
            log.error("Error fetching job execution for ID: {}", jobExecutionId, e);
        }
        return null;
    }

    private JobExecutionInfo mapToJobExecutionInfo(JobExecution jobExecution) {
        String fileName = jobExecution.getJobParameters().getString("originalFileName");
        if (fileName == null) {
            fileName = "Unknown";
        }

        BatchStatus status = jobExecution.getStatus();
        long writeCount = jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getWriteCount())
                .sum();

        long readCount = jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getReadCount())
                .sum();

        long failedCount = readCount > 0 ? readCount - writeCount : 0;

        String startTime = jobExecution.getStartTime() != null
                ? jobExecution.getStartTime().format(FORMATTER)
                : "N/A";

        String endTime = jobExecution.getEndTime() != null
                ? jobExecution.getEndTime().format(FORMATTER)
                : "N/A";

        int progressPercentage = calculateProgress(jobExecution);

        return JobExecutionInfo.builder()
                .jobId(jobExecution.getId())
                .fileName(fileName)
                .status(status.name())
                .successCount(writeCount)
                .failedCount(failedCount)
                .startTime(startTime)
                .endTime(endTime)
                .progressPercentage(progressPercentage)
                .build();
    }

    private int calculateProgress(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            return 100;
        }

        long readCount = jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getReadCount())
                .sum();

        long writeCount = jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getWriteCount())
                .sum();

        if (readCount > 0) {
            return (int) ((writeCount * 100) / readCount);
        }

        return 0;
    }
}
