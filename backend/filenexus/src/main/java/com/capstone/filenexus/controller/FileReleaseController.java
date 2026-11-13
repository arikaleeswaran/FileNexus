
package com.capstone.filenexus.controller;

import com.capstone.filenexus.dto.response.ApiResponse;
import com.capstone.filenexus.service.FileReleaseService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/file-release")
public class FileReleaseController {

    private final FileReleaseService fileReleaseService;

    public FileReleaseController(
            FileReleaseService fileReleaseService
    ) {
        this.fileReleaseService = fileReleaseService;
    }

    @PostMapping("/{scanId}")
    public ResponseEntity<ApiResponse<Void>> releaseFile(
            @PathVariable Long scanId
    ) {
        fileReleaseService.releaseCleanFile(scanId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "File successfully released to clean storage",
                        null
                )
        );
    }
}
