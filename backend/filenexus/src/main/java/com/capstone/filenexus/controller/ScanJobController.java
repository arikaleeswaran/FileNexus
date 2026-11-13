
package com.capstone.filenexus.controller;

import com.capstone.filenexus.dto.request.CreateScanJobRequest;
import com.capstone.filenexus.dto.request.CompleteScanRequest;
import com.capstone.filenexus.dto.request.ScanFailureRequest;
import com.capstone.filenexus.dto.response.ApiResponse;
import com.capstone.filenexus.dto.response.ScanJobResponse;
import com.capstone.filenexus.service.ScanJobService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scan-jobs")
public class ScanJobController {

    private final ScanJobService scanJobService;

    public ScanJobController(ScanJobService scanJobService) {
        this.scanJobService = scanJobService;
    }

    // Create scan job for a file
    @PostMapping("/files/{fileId}")
    public ResponseEntity<ApiResponse<ScanJobResponse>> createScanJob(
            @PathVariable Long fileId,
            @Valid @RequestBody CreateScanJobRequest request
    ) {

        ScanJobResponse response =
                scanJobService.createScanJob(fileId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Scan job created successfully",
                        response
                ));
    }

    // Get scan job by ID
    @GetMapping("/{scanId}")
    public ResponseEntity<ApiResponse<ScanJobResponse>> getScanJob(
            @PathVariable Long scanId
    ) {

        ScanJobResponse response =
                scanJobService.getScanJobById(scanId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Scan job retrieved successfully",
                        response
                )
        );
    }

    // Get scan history for a file
    @GetMapping("/files/{fileId}")
    public ResponseEntity<ApiResponse<List<ScanJobResponse>>> getScanHistory(
            @PathVariable Long fileId
    ) {

        List<ScanJobResponse> response =
                scanJobService.getScanHistory(fileId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Scan history retrieved successfully",
                        response
                )
        );
    }

    // Start scan
    @PatchMapping("/{scanId}/start")
    public ResponseEntity<ApiResponse<ScanJobResponse>> startScan(
            @PathVariable Long scanId
    ) {

        ScanJobResponse response =
                scanJobService.startScan(scanId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Scan job started successfully",
                        response
                )
        );
    }

    // Complete scan
    @PatchMapping("/{scanId}/complete")
    public ResponseEntity<ApiResponse<ScanJobResponse>> completeScan(
            @PathVariable Long scanId,
            @Valid @RequestBody CompleteScanRequest request
    ) {

        ScanJobResponse response =
                scanJobService.completeScan(scanId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Scan job completed successfully",
                        response
                )
        );
    }

    // Mark scan as failed
    @PatchMapping("/{scanId}/fail")
    public ResponseEntity<ApiResponse<ScanJobResponse>> failScan(
            @PathVariable Long scanId,
            @Valid @RequestBody ScanFailureRequest request
    ) {

        ScanJobResponse response =
                scanJobService.failScan(scanId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Scan job marked as failed",
                        response
                )
        );
    }

    // Mark scan as timed out
    @PatchMapping("/{scanId}/timeout")
    public ResponseEntity<ApiResponse<ScanJobResponse>> timeoutScan(
            @PathVariable Long scanId,
            @Valid @RequestBody ScanFailureRequest request
    ) {

        ScanJobResponse response =
                scanJobService.timeoutScan(scanId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Scan job timed out",
                        response
                )
        );
    }
}
