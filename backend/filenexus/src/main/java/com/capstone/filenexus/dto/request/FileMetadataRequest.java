package com.capstone.filenexus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class FileMetadataRequest {

    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name cannot exceed 255 characters")
    private String originalName;

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be greater than zero")
    private Long fileSize;

    @Size(max = 150, message = "MIME type cannot exceed 150 characters")
    private String declaredMimeType;

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDeclaredMimeType() {
        return declaredMimeType;
    }

    public void setDeclaredMimeType(String declaredMimeType) {
        this.declaredMimeType = declaredMimeType;
    }
}