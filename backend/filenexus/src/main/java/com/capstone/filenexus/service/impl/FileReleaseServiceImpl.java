
package com.capstone.filenexus.service.impl;

import com.capstone.filenexus.entity.FileEntity;
import com.capstone.filenexus.entity.ScanJob;
import com.capstone.filenexus.entity.enums.ActorType;
import com.capstone.filenexus.entity.enums.FileStatus;
import com.capstone.filenexus.entity.enums.ScanResult;
import com.capstone.filenexus.entity.enums.ScanStatus;
import com.capstone.filenexus.exception.BadRequestException;
import com.capstone.filenexus.exception.ResourceNotFoundException;
import com.capstone.filenexus.repository.ScanJobRepository;
import com.capstone.filenexus.repository.FileRepository;
import com.capstone.filenexus.service.AuditLogService;
import com.capstone.filenexus.service.FileReleaseService;
import com.capstone.filenexus.service.S3StorageService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
public class FileReleaseServiceImpl implements FileReleaseService {

    private final ScanJobRepository scanJobRepository;
    private final FileRepository fileRepository;
    private final S3StorageService s3StorageService;
    private final AuditLogService auditLogService;

    public FileReleaseServiceImpl(
            ScanJobRepository scanJobRepository,
            FileRepository fileRepository,
            S3StorageService s3StorageService,
            AuditLogService auditLogService
    ) {
        this.scanJobRepository = scanJobRepository;
        this.fileRepository = fileRepository;
        this.s3StorageService = s3StorageService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(noRollbackFor = IllegalStateException.class)
    public void releaseCleanFile(Long scanId) {

        // 1. Retrieve the scan job.
        ScanJob scanJob = scanJobRepository.findById(scanId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Scan job not found with ID: " + scanId
                        )
                );

        FileEntity file = scanJob.getFile();

        // 2. Make repeated release requests safe.
        if (file.getStatus() == FileStatus.APPROVED) {
            return;
        }

        // 3. Verify the scan has actually completed.
        if (scanJob.getScanStatus() != ScanStatus.COMPLETED) {
            throw new BadRequestException(
                    "Only completed scan jobs can be released"
            );
        }

        // 4. Only CLEAN files can be released.
        if (scanJob.getScanResult() != ScanResult.CLEAN) {
            throw new BadRequestException(
                    "Only files with a CLEAN scan result can be released"
            );
        }

        // 5. Permit retry after a previous copy failure.
        if (file.getStatus() != FileStatus.SCANNING
                && file.getStatus() != FileStatus.COPY_FAILED) {
            throw new BadRequestException(
                    "File cannot be released in its current status: "
                            + file.getStatus()
            );
        }

        if (file.getQuarantineKey() == null
                || file.getSha256Checksum() == null
                || file.getFileSize() == null) {
            throw new BadRequestException(
                    "Required file metadata is missing for release"
            );
        }

        String cleanKey = file.getCleanKey();

        if (cleanKey == null || cleanKey.isBlank()) {
            cleanKey = "clean/"
                    + LocalDate.now().getYear()
                    + "/"
                    + String.format("%02d", LocalDate.now().getMonthValue())
                    + "/"
                    + file.getFileId()
                    + "/"
                    + file.getSha256Checksum();
        }

        // 6. Copy and verify the object.
        try {
            s3StorageService.copyQuarantineToClean(
                    file.getQuarantineKey(),
                    cleanKey,
                    file.getFileId(),
                    file.getFileSize(),
                    file.getSha256Checksum()
            );

            // 7. Update database only after S3 verification succeeds.
            file.setCleanKey(cleanKey);
            file.setStatus(FileStatus.APPROVED);

            fileRepository.save(file);

            // 8. Record successful release.
            auditLogService.recordEvent(
                    null,
                    file.getFileId(),
                    ActorType.SYSTEM,
                    "FILE_RELEASED_TO_CLEAN_BUCKET",
                    FileStatus.SCANNING.name(),
                    FileStatus.APPROVED.name(),
                    Map.of(
                            "scanId", scanId,
                            "quarantineKey", file.getQuarantineKey(),
                            "cleanKey", cleanKey,
                            "sha256Checksum", file.getSha256Checksum()
                    ),
                    UUID.randomUUID().toString()
            );

        } catch (RuntimeException exception) {

            // Preserve the original quarantine object.
            file.setStatus(FileStatus.COPY_FAILED);
            fileRepository.save(file);

            auditLogService.recordEvent(
                    null,
                    file.getFileId(),
                    ActorType.SYSTEM,
                    "FILE_RELEASE_COPY_FAILED",
                    FileStatus.SCANNING.name(),
                    FileStatus.COPY_FAILED.name(),
                    Map.of(
                            "scanId", scanId,
                            "reason", exception.getMessage() == null
                                    ? "Unknown S3 copy failure"
                                    : exception.getMessage()
                    ),
                    UUID.randomUUID().toString()
            );

            throw new IllegalStateException(
                    "File scan was CLEAN, but clean-bucket release failed",
                    exception
            );
        }
    }
}
