package com.capstone.filenexus.dto.response;

import com.capstone.filenexus.entity.enums.FileStatus;

import java.util.List;

public class FileValidationResponse {

    private Long fileId;
    private String originalName;
    private FileStatus status;
    private String detectedMimeType;
    private String sha256Checksum;
    private boolean valid;
    private List<ValidationCheckResponse> checks;

    public FileValidationResponse(
            Long fileId,
            String originalName,
            FileStatus status,
            String detectedMimeType,
            String sha256Checksum,
            boolean valid,
            List<ValidationCheckResponse> checks
    ) {
        this.fileId = fileId;
        this.originalName = originalName;
        this.status = status;
        this.detectedMimeType = detectedMimeType;
        this.sha256Checksum = sha256Checksum;
        this.valid = valid;
        this.checks = checks;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public FileStatus getStatus() {
        return status;
    }

    public String getDetectedMimeType() {
        return detectedMimeType;
    }

    public String getSha256Checksum() {
        return sha256Checksum;
    }

    public boolean isValid() {
        return valid;
    }

    public List<ValidationCheckResponse> getChecks() {
        return checks;
    }
}