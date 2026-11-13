package com.capstone.filenexus.controller;

import com.capstone.filenexus.dto.response.ApiResponse;
import com.capstone.filenexus.dto.response.FileValidationResponse;
import com.capstone.filenexus.dto.response.ValidationCheckResponse;
import com.capstone.filenexus.service.FileValidationService;

import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@Validated
public class FileValidationController {

    private final FileValidationService fileValidationService;

    public FileValidationController(
            FileValidationService fileValidationService
    ) {
        this.fileValidationService = fileValidationService;
    }

    @PostMapping(
            value = "/{fileId}/validate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<FileValidationResponse>> validateFile(
            @PathVariable @Positive Long fileId,
            @RequestParam("file") MultipartFile file
    ) {

        FileValidationResponse response =
                fileValidationService.validateFile(fileId, file);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "File validation completed",
                        response
                )
        );
    }

    @GetMapping("/{fileId}/validation-history")
    public ResponseEntity<ApiResponse<List<ValidationCheckResponse>>>
    getValidationHistory(
            @PathVariable @Positive Long fileId
    ) {

        List<ValidationCheckResponse> history =
                fileValidationService.getValidationHistory(fileId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Validation history retrieved successfully",
                        history
                )
        );
    }
}