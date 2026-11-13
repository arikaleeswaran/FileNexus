
package com.capstone.filenexus.service.impl;

import com.capstone.filenexus.dto.request.CreateScanJobRequest;
import com.capstone.filenexus.dto.request.CompleteScanRequest;
import com.capstone.filenexus.dto.request.ScanFailureRequest;
import com.capstone.filenexus.dto.response.ScanJobResponse;
import com.capstone.filenexus.entity.FileEntity;
import com.capstone.filenexus.entity.ScanJob;
import com.capstone.filenexus.entity.enums.FileStatus;
import com.capstone.filenexus.entity.enums.ScanResult;
import com.capstone.filenexus.entity.enums.ScanStatus;
import com.capstone.filenexus.exception.BadRequestException;
import com.capstone.filenexus.exception.ResourceNotFoundException;
import com.capstone.filenexus.repository.FileRepository;
import com.capstone.filenexus.repository.ScanJobRepository;
import com.capstone.filenexus.service.ScanJobService;

import com.capstone.filenexus.entity.enums.ActorType;
import com.capstone.filenexus.service.AuditLogService;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class ScanJobServiceImpl implements ScanJobService {

    private final ScanJobRepository scanJobRepository;
    private final FileRepository fileRepository;
    private final AuditLogService auditLogService;

    public ScanJobServiceImpl(
            ScanJobRepository scanJobRepository,
            FileRepository fileRepository,
            AuditLogService auditLogService
    ) {
        this.scanJobRepository = scanJobRepository;
        this.fileRepository = fileRepository;
        this.auditLogService = auditLogService;
    }

    // 1. Create a new scan job
    @Override
    public ScanJobResponse createScanJob(
            Long fileId,
            CreateScanJobRequest request
    ) {

        String idempotencyKey = request.getIdempotencyKey().trim();

        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "File not found with ID: " + fileId
                        )
                );

        // Check for an existing request
        var existingJob = scanJobRepository
                .findByIdempotencyKey(idempotencyKey);

        if (existingJob.isPresent()) {

            ScanJob previousJob = existingJob.get();

            if (!previousJob.getFile().getFileId().equals(fileId)) {
                throw new BadRequestException(
                        "Idempotency key is already associated with another file"
                );
            }

            // Return the original job for a duplicate request
            return mapToResponse(previousJob);
        }

        // A file must be quarantined before scanning
        if (file.getStatus() != FileStatus.QUARANTINED) {
            throw new BadRequestException(
                    "Only quarantined files can be submitted for scanning"
            );
        }

        // Calculate the next attempt number
        int attemptNumber = scanJobRepository
                .findTopByFileFileIdOrderByAttemptNumberDesc(fileId)
                .map(job -> job.getAttemptNumber() + 1)
                .orElse(1);

        ScanJob scanJob = new ScanJob();

        scanJob.setFile(file);
        scanJob.setAttemptNumber(attemptNumber);
        scanJob.setIdempotencyKey(idempotencyKey);
        scanJob.setScanStatus(ScanStatus.QUEUED);

        ScanJob savedJob = scanJobRepository.save(scanJob);

        return mapToResponse(savedJob);
    }

    // 2. Retrieve scan job by ID
    @Override
    @Transactional(readOnly = true)
    public ScanJobResponse getScanJobById(Long scanId) {

        ScanJob scanJob = findScanJob(scanId);

        return mapToResponse(scanJob);
    }

    // 3. Retrieve scan history for a file
    @Override
    @Transactional(readOnly = true)
    public List<ScanJobResponse> getScanHistory(Long fileId) {

        if (!fileRepository.existsById(fileId)) {
            throw new ResourceNotFoundException(
                    "File not found with ID: " + fileId
            );
        }

        return scanJobRepository
                .findByFileFileIdOrderByAttemptNumberDesc(fileId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // 4. Start scanning
    @Override
    public ScanJobResponse startScan(Long scanId) {

        ScanJob scanJob = findScanJob(scanId);

        if (scanJob.getScanStatus() != ScanStatus.QUEUED) {
            throw new BadRequestException(
                    "Only queued scan jobs can be started"
            );
        }

        scanJob.setScanStatus(ScanStatus.RUNNING);
        scanJob.setStartedAt(Instant.now());

        auditLogService.recordEvent(
                null,
                scanJob.getFile().getFileId(),
                ActorType.SYSTEM,
                "SCAN_STARTED",
                FileStatus.QUARANTINED.name(),
                FileStatus.SCANNING.name(),
                Map.of(
                        "scanId", scanJob.getScanId(),
                        "attemptNumber", scanJob.getAttemptNumber()
                ),
                UUID.randomUUID().toString()
        );

        FileEntity file = scanJob.getFile();
        file.setStatus(FileStatus.SCANNING);

        return mapToResponse(scanJob);
    }

    // 5. Complete scanning
    @Override
    public ScanJobResponse completeScan(
            Long scanId,
            CompleteScanRequest request
    ) {

        ScanJob scanJob = findScanJob(scanId);

        if (scanJob.getScanStatus() != ScanStatus.RUNNING) {
            throw new BadRequestException(
                    "Only running scan jobs can be completed"
            );
        }

        Instant completedAt = Instant.now();

        scanJob.setScanStatus(ScanStatus.COMPLETED);
        scanJob.setScanResult(request.getScanResult());
        scanJob.setCompletedAt(completedAt);
        scanJob.setScannerVersion(request.getScannerVersion());

        calculateDuration(scanJob, completedAt);

        auditLogService.recordEvent(
                null,
                scanJob.getFile().getFileId(),
                ActorType.SCANNER,
                "SCAN_COMPLETED",
                FileStatus.SCANNING.name(),
                scanJob.getFile().getStatus().name(),
                Map.of(
                        "scanId", scanJob.getScanId(),
                        "scanResult", request.getScanResult().name(),
                        "scannerVersion",
                        request.getScannerVersion() == null
                                ? "unknown"
                                : request.getScannerVersion()
                ),
                UUID.randomUUID().toString()
        );

        FileEntity file = scanJob.getFile();

        if (request.getScanResult() == ScanResult.REJECTED) {

            file.setStatus(FileStatus.REJECTED);

        } else if (request.getScanResult() == ScanResult.CLEAN) {

            // Keep file in SCANNING state until the backend
            // copies it to clean storage and verifies the copy.
            // Only then can it become APPROVED.
        }

        return mapToResponse(scanJob);
    }

    // 6. Mark scan as failed
    @Override
    public ScanJobResponse failScan(
            Long scanId,
            ScanFailureRequest request
    ) {

        ScanJob scanJob = findScanJob(scanId);

        if (scanJob.getScanStatus() != ScanStatus.RUNNING
                && scanJob.getScanStatus() != ScanStatus.QUEUED) {

            throw new BadRequestException(
                    "Only queued or running scan jobs can fail"
            );
        }

        Instant completedAt = Instant.now();

        scanJob.setScanStatus(ScanStatus.FAILED);
        scanJob.setFailureReason(request.getFailureReason());
        scanJob.setCompletedAt(completedAt);

        if (scanJob.getStartedAt() != null) {
            calculateDuration(scanJob, completedAt);
        }

        scanJob.getFile().setStatus(FileStatus.SCAN_FAILED);

        auditLogService.recordEvent(
                null,
                scanJob.getFile().getFileId(),
                ActorType.SYSTEM,
                "SCAN_FAILED",
                FileStatus.QUARANTINED.name(),
                FileStatus.SCANNING.name(),
                Map.of(
                        "scanId", scanJob.getScanId(),
                        "attemptNumber", scanJob.getAttemptNumber()
                ),
                UUID.randomUUID().toString()
        );

        return mapToResponse(scanJob);
    }

    // 7. Mark scan as timed out
    @Override
    public ScanJobResponse timeoutScan(
            Long scanId,
            ScanFailureRequest request
    ) {

        ScanJob scanJob = findScanJob(scanId);

        if (scanJob.getScanStatus() != ScanStatus.RUNNING
                && scanJob.getScanStatus() != ScanStatus.QUEUED) {

            throw new BadRequestException(
                    "Only queued or running scan jobs can time out"
            );
        }

        Instant completedAt = Instant.now();

        scanJob.setScanStatus(ScanStatus.TIMEOUT);
        scanJob.setFailureReason(request.getFailureReason());
        scanJob.setCompletedAt(completedAt);

        if (scanJob.getStartedAt() != null) {
            calculateDuration(scanJob, completedAt);
        }

        scanJob.getFile().setStatus(FileStatus.SCAN_TIMEOUT);

        auditLogService.recordEvent(
                null,
                scanJob.getFile().getFileId(),
                ActorType.SYSTEM,
                "SCAN_TIMEOUT",
                FileStatus.QUARANTINED.name(),
                FileStatus.SCANNING.name(),
                Map.of(
                        "scanId", scanJob.getScanId(),
                        "attemptNumber", scanJob.getAttemptNumber()
                ),
                UUID.randomUUID().toString()
        );

        return mapToResponse(scanJob);
    }

    // Find scan job or throw 404
    private ScanJob findScanJob(Long scanId) {

        return scanJobRepository.findById(scanId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Scan job not found with ID: " + scanId
                        )
                );
    }

    // Calculate execution time
    private void calculateDuration(
            ScanJob scanJob,
            Instant completedAt
    ) {

        if (scanJob.getStartedAt() != null) {

            long duration = Duration.between(
                    scanJob.getStartedAt(),
                    completedAt
            ).toMillis();

            scanJob.setDurationMs(Math.max(0, duration));
        }
    }

    // Entity to response DTO
    private ScanJobResponse mapToResponse(ScanJob scanJob) {

        return new ScanJobResponse(
                scanJob.getScanId(),
                scanJob.getFile().getFileId(),
                scanJob.getAttemptNumber(),
                scanJob.getScanStatus(),
                scanJob.getScanResult(),
                scanJob.getStartedAt(),
                scanJob.getCompletedAt(),
                scanJob.getDurationMs(),
                scanJob.getFailureReason(),
                scanJob.getScannerVersion(),
                scanJob.getCreatedAt()
        );
    }
}
