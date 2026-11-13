
package com.capstone.filenexus.dto.response;

import com.capstone.filenexus.entity.enums.ScanResult;
import com.capstone.filenexus.entity.enums.ScanStatus;

import java.time.Instant;

public class ScanJobResponse {

    private Long scanId;
    private Long fileId;
    private Integer attemptNumber;
    private ScanStatus scanStatus;
    private ScanResult scanResult;
    private Instant startedAt;
    private Instant completedAt;
    private Long durationMs;
    private String failureReason;
    private String scannerVersion;
    private Instant createdAt;

    public ScanJobResponse(
            Long scanId,
            Long fileId,
            Integer attemptNumber,
            ScanStatus scanStatus,
            ScanResult scanResult,
            Instant startedAt,
            Instant completedAt,
            Long durationMs,
            String failureReason,
            String scannerVersion,
            Instant createdAt
    ) {
        this.scanId = scanId;
        this.fileId = fileId;
        this.attemptNumber = attemptNumber;
        this.scanStatus = scanStatus;
        this.scanResult = scanResult;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.durationMs = durationMs;
        this.failureReason = failureReason;
        this.scannerVersion = scannerVersion;
        this.createdAt = createdAt;
    }

    public Long getScanId() {
        return scanId;
    }

    public Long getFileId() {
        return fileId;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public ScanStatus getScanStatus() {
        return scanStatus;
    }

    public ScanResult getScanResult() {
        return scanResult;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getScannerVersion() {
        return scannerVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
