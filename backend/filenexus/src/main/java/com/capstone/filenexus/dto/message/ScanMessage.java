package com.capstone.filenexus.dto.message;

public class ScanMessage {

    private Long fileId;
    private Long scanId;
    private String quarantineBucket;
    private String quarantineKey;
    private String sha256Checksum;
    private String detectedMimeType;
    private String originalName;
    private int attemptNumber;
    private String correlationId;

    public ScanMessage() {
    }

    public ScanMessage(
            Long fileId,
            Long scanId,
            String quarantineBucket,
            String quarantineKey,
            String sha256Checksum,
            String detectedMimeType,
            String originalName,
            int attemptNumber,
            String correlationId
    ) {
        this.fileId = fileId;
        this.scanId = scanId;
        this.quarantineBucket = quarantineBucket;
        this.quarantineKey = quarantineKey;
        this.sha256Checksum = sha256Checksum;
        this.detectedMimeType = detectedMimeType;
        this.originalName = originalName;
        this.attemptNumber = attemptNumber;
        this.correlationId = correlationId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getScanId() {
        return scanId;
    }

    public void setScanId(Long scanId) {
        this.scanId = scanId;
    }

    public String getQuarantineBucket() {
        return quarantineBucket;
    }

    public void setQuarantineBucket(String quarantineBucket) {
        this.quarantineBucket = quarantineBucket;
    }

    public String getQuarantineKey() {
        return quarantineKey;
    }

    public void setQuarantineKey(String quarantineKey) {
        this.quarantineKey = quarantineKey;
    }

    public String getSha256Checksum() {
        return sha256Checksum;
    }

    public void setSha256Checksum(String sha256Checksum) {
        this.sha256Checksum = sha256Checksum;
    }

    public String getDetectedMimeType() {
        return detectedMimeType;
    }

    public void setDetectedMimeType(String detectedMimeType) {
        this.detectedMimeType = detectedMimeType;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}