package com.capstone.filenexus.dto.response;

import com.capstone.filenexus.entity.enums.FileStatus;

import java.time.Instant;

public class FileResponse {

    private Long fileId;
    private String originalName;
    private Long fileSize;
    private String declaredMimeType;
    private String detectedMimeType;
    private String sha256Checksum;
    private FileStatus status;
    private Instant uploadedAt;

    public FileResponse() {
    }

    public FileResponse(
            Long fileId,
            String originalName,
            Long fileSize,
            String declaredMimeType,
            String detectedMimeType,
            String sha256Checksum,
            FileStatus status,
            Instant uploadedAt
    ) {
        this.fileId = fileId;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.declaredMimeType = declaredMimeType;
        this.detectedMimeType = detectedMimeType;
        this.sha256Checksum = sha256Checksum;
        this.status = status;
        this.uploadedAt = uploadedAt;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getDeclaredMimeType() {
        return declaredMimeType;
    }

    public String getDetectedMimeType() {
        return detectedMimeType;
    }

    public String getSha256Checksum() {
        return sha256Checksum;
    }

    public FileStatus getStatus() {
        return status;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}