
package com.capstone.filenexus.controller;

import com.capstone.filenexus.dto.response.ApiResponse;
import com.capstone.filenexus.dto.response.FileUploadResponse;
import com.capstone.filenexus.entity.enums.UserRole;
import com.capstone.filenexus.service.FileUploadService;

import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@Validated
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping(
            value = "/{fileId}/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @PathVariable @Positive Long fileId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {

        UserRole role = getCurrentRole(authentication);

        FileUploadResponse response = fileUploadService.uploadFile(
                fileId,
                file,
                authentication.getName(),
                role
        );

        if (!response.isUploaded()) {
            return ResponseEntity.unprocessableEntity()
                    .body(new ApiResponse<>(
                            false,
                            "File validation failed. File was not uploaded to S3.",
                            response
                    ));
        }

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "File validated and uploaded to quarantine successfully",
                        response
                )
        );
    }

    private UserRole getCurrentRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority ->
                        UserRole.valueOf(authority.substring(5)))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("User role is missing"));
    }
}
