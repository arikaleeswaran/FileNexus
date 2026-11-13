package com.capstone.filenexus.dto.response;

import com.capstone.filenexus.entity.enums.FileStatus;

public class FileUploadResponse {

    private Long fileId;
    private String originalName;
    private FileStatus status;
    private String quarantineKey;
    private String sha256Checksum;
    private boolean uploaded;

    public FileUploadResponse(
            Long fileId,
            String originalName,
            FileStatus status,
            String quarantineKey,
            String sha256Checksum,
            boolean uploaded
    ) {
        this.fileId = fileId;
        this.originalName = originalName;
        this.status = status;
        this.quarantineKey = quarantineKey;
        this.sha256Checksum = sha256Checksum;
        this.uploaded = uploaded;
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

    public String getQuarantineKey() {
        return quarantineKey;
    }

    public String getSha256Checksum() {
        return sha256Checksum;
    }

    public boolean isUploaded() {
        return uploaded;
    }
}