package com.capstone.filenexus.service;

import com.capstone.filenexus.dto.response.FileValidationResponse;
import com.capstone.filenexus.dto.response.ValidationCheckResponse;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface FileValidationService {

    FileValidationResponse validateFile(
            Long fileId,
            MultipartFile file
    );

    List<ValidationCheckResponse> getValidationHistory(Long fileId);
}