
package com.capstone.filenexus.service.impl;

import com.capstone.filenexus.dto.message.ScanMessage;
import com.capstone.filenexus.dto.request.CreateScanJobRequest;
import com.capstone.filenexus.dto.response.FileValidationResponse;
import com.capstone.filenexus.dto.response.FileUploadResponse;
import com.capstone.filenexus.dto.response.ScanJobResponse;
import com.capstone.filenexus.entity.FileEntity;
import com.capstone.filenexus.entity.enums.ActorType;
import com.capstone.filenexus.entity.enums.FileStatus;
import com.capstone.filenexus.entity.enums.UserRole;
import com.capstone.filenexus.exception.BadRequestException;
import com.capstone.filenexus.exception.ResourceNotFoundException;
import com.capstone.filenexus.repository.FileRepository;
import com.capstone.filenexus.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.util.HexFormat;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Logger log =
            LoggerFactory.getLogger(FileUploadServiceImpl.class);

    private final FileRepository fileRepository;
    private final FileValidationService fileValidationService;
    private final S3StorageService s3StorageService;
    private final AuditLogService auditLogService;
    private final ScanJobService scanJobService;
    private final SqsMessageProducer sqsMessageProducer;
    private final FileAuthorizationService fileAuthorizationService;

    @Value("${filenexus.aws.s3.quarantine-bucket}")
    private String quarantineBucket;

    public FileUploadServiceImpl(
            FileRepository fileRepository,
            FileValidationService fileValidationService,
            S3StorageService s3StorageService,
            AuditLogService auditLogService,
            ScanJobService scanJobService,
            SqsMessageProducer sqsMessageProducer,
            FileAuthorizationService fileAuthorizationService
    ) {
        this.fileRepository = fileRepository;
        this.fileValidationService = fileValidationService;
        this.s3StorageService = s3StorageService;
        this.auditLogService = auditLogService;
        this.scanJobService = scanJobService;
        this.sqsMessageProducer = sqsMessageProducer;
        this.fileAuthorizationService = fileAuthorizationService;
    }

    @Override
    public FileUploadResponse uploadFile(
            Long fileId,
            MultipartFile uploadedFile,
            String email,
            UserRole role
    ) {

        FileEntity file = fileAuthorizationService.getFileForModification(
                fileId,
                email,
                role
        );

        // Validate only when validation has not already succeeded.
        if (file.getStatus() == FileStatus.UPLOADED
                || file.getStatus() == FileStatus.VALIDATION_FAILED) {

            FileValidationResponse validation =
                    fileValidationService.validateFile(fileId, uploadedFile);

            if (!validation.isValid()) {
                return new FileUploadResponse(
                        validation.getFileId(),
                        validation.getOriginalName(),
                        validation.getStatus(),
                        null,
                        validation.getSha256Checksum(),
                        false
                );
            }

            file = fileRepository.findById(fileId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "File not found with ID: " + fileId
                            )
                    );

        } else if (file.getStatus() != FileStatus.VALIDATED) {

            throw new BadRequestException(
                    "File cannot be uploaded in its current status: "
                            + file.getStatus()
            );
        }

        // Verify that the uploaded content matches the validated content.
        if (uploadedFile == null || uploadedFile.isEmpty()
                || uploadedFile.getSize() != file.getFileSize()) {

            throw new BadRequestException(
                    "Uploaded file does not match the validated file size"
            );
        }

        if (file.getSha256Checksum() == null
                || file.getDetectedMimeType() == null) {

            throw new BadRequestException(
                    "Validated checksum or MIME type is missing"
            );
        }

        String uploadedChecksum = calculateSha256(uploadedFile);

        if (!uploadedChecksum.equalsIgnoreCase(file.getSha256Checksum())) {
            throw new BadRequestException(
                    "Uploaded content does not match the previously validated file"
            );
        }

        LocalDate today = LocalDate.now();

        String objectKey = String.format(
                "quarantine/%d/%02d/%d/%s",
                today.getYear(),
                today.getMonthValue(),
                fileId,
                UUID.randomUUID()
        );

        // STEP 1: Upload the validated file to S3 quarantine.
        s3StorageService.uploadToQuarantine(
                uploadedFile,
                objectKey,
                fileId,
                file.getSha256Checksum(),
                file.getDetectedMimeType()
        );

        // STEP 2: Persist the quarantine location and file status.
        FileEntity savedFile;

        try {
            file.setQuarantineKey(objectKey);
            file.setStatus(FileStatus.QUARANTINED);

            savedFile = fileRepository.save(file);

        } catch (RuntimeException exception) {

            // The database save failed, so clean up the S3 object.
            try {
                s3StorageService.deleteQuarantineObject(objectKey);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            throw exception;
        }

        // STEP 3: Generate one correlation ID for this workflow.
        String correlationId = UUID.randomUUID().toString();

        // STEP 4: Create a queued scan job.
        CreateScanJobRequest scanJobRequest = new CreateScanJobRequest();
        scanJobRequest.setIdempotencyKey(
                "file-" + savedFile.getFileId()
                        + "-scan-" + UUID.randomUUID()
        );

        ScanJobResponse scanJob = scanJobService.createScanJob(
                savedFile.getFileId(),
                scanJobRequest
        );

        // STEP 5: Record the upload and scan-job creation in the audit log.
        auditLogService.recordEvent(
                null,
                savedFile.getFileId(),
                ActorType.SYSTEM,
                "FILE_UPLOADED_TO_QUARANTINE",
                FileStatus.VALIDATED.name(),
                FileStatus.QUARANTINED.name(),
                Map.of(
                        "quarantineKey", objectKey,
                        "fileSize", savedFile.getFileSize(),
                        "sha256Checksum", savedFile.getSha256Checksum(),
                        "scanId", scanJob.getScanId()
                ),
                correlationId
        );

        // STEP 6: Build the SQS message.
        ScanMessage scanMessage = new ScanMessage();

        scanMessage.setFileId(savedFile.getFileId());
        scanMessage.setScanId(scanJob.getScanId());
        scanMessage.setQuarantineBucket(quarantineBucket);
        scanMessage.setQuarantineKey(savedFile.getQuarantineKey());
        scanMessage.setSha256Checksum(savedFile.getSha256Checksum());
        scanMessage.setDetectedMimeType(savedFile.getDetectedMimeType());
        scanMessage.setOriginalName(savedFile.getOriginalName());
        scanMessage.setAttemptNumber(scanJob.getAttemptNumber());
        scanMessage.setCorrelationId(correlationId);

        // STEP 7: Publish the message to AWS SQS.
        try {
            String messageId =
                    sqsMessageProducer.sendScanMessage(scanMessage);

            log.info(
                    "Scan message published successfully. fileId={}, scanId={}, messageId={}",
                    savedFile.getFileId(),
                    scanJob.getScanId(),
                    messageId
            );

        } catch (RuntimeException exception) {

            // Do NOT delete the S3 object.
            // Do NOT mark the file as SCAN_FAILED.
            // The scan job remains QUEUED because the scanner has not run.

            log.error(
                    "SQS publishing failed. File remains quarantined and scan job remains queued. fileId={}, scanId={}",
                    savedFile.getFileId(),
                    scanJob.getScanId(),
                    exception
            );

            throw new IllegalStateException(
                    "File was uploaded to quarantine and scan job was created, "
                            + "but the SQS message could not be published. "
                            + "The scan job remains queued and requires dispatch retry.",
                    exception
            );
        }

        // STEP 8: Return the existing upload response.
        return new FileUploadResponse(
                savedFile.getFileId(),
                savedFile.getOriginalName(),
                savedFile.getStatus(),
                savedFile.getQuarantineKey(),
                savedFile.getSha256Checksum(),
                true
        );
    }

    private String calculateSha256(MultipartFile file) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream inputStream = file.getInputStream();
                 DigestInputStream digestInputStream =
                         new DigestInputStream(inputStream, digest)) {

                byte[] buffer = new byte[8192];

                while (digestInputStream.read(buffer) != -1) {
                    // Digest is updated as the stream is read.
                }
            }

            return HexFormat.of().formatHex(digest.digest());

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to verify uploaded file checksum",
                    exception
            );
        }
    }
}
