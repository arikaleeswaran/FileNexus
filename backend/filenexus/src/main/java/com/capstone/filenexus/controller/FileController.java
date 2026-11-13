
package com.capstone.filenexus.controller;

import com.capstone.filenexus.dto.request.FileMetadataRequest;
import com.capstone.filenexus.dto.response.ApiResponse;
import com.capstone.filenexus.dto.response.FileResponse;
import com.capstone.filenexus.dto.response.PageResponse;
import com.capstone.filenexus.entity.enums.FileStatus;
import com.capstone.filenexus.entity.enums.UserRole;
import com.capstone.filenexus.service.FileService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // Create file metadata for the authenticated user.
    @PostMapping("/metadata")
    public ResponseEntity<ApiResponse<FileResponse>> createFileMetadata(
            @Valid @RequestBody FileMetadataRequest request,
            Authentication authentication
    ) {

        FileResponse response =
                fileService.createFileMetadata(
                        request,
                        authentication.getName()
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "File metadata created successfully",
                        response
                ));
    }

    // Retrieve a file using ownership and role checks.
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileResponse>> getFileById(
            @PathVariable Long fileId,
            Authentication authentication
    ) {

        UserRole role = getCurrentRole(authentication);

        FileResponse response = fileService.getFileById(
                fileId,
                authentication.getName(),
                role
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "File retrieved successfully",
                        response
                )
        );
    }

    // Retrieve files with pagination and ownership restrictions.
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FileResponse>>> getFiles(

            @RequestParam(required = false)
            FileStatus status,

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant toDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "uploadedAt")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String sortDir,

            Authentication authentication
    ) {

        UserRole role = getCurrentRole(authentication);

        PageResponse<FileResponse> files = fileService.getFiles(
                status,
                keyword,
                fromDate,
                toDate,
                page,
                size,
                sortBy,
                sortDir,
                authentication.getName(),
                role
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Files retrieved successfully",
                        files
                )
        );
    }

    private UserRole getCurrentRole(Authentication authentication) {

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority ->
                        UserRole.valueOf(authority.substring(5))
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user role is missing"
                        )
                );
    }
}
