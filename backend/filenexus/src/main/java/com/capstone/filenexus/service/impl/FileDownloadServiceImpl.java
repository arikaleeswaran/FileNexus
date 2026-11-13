
package com.capstone.filenexus.service.impl;

import com.capstone.filenexus.entity.FileEntity;
import com.capstone.filenexus.entity.enums.AccessResult;
import com.capstone.filenexus.entity.enums.AccessType;
import com.capstone.filenexus.entity.enums.FileStatus;
import com.capstone.filenexus.entity.enums.UserRole;
import com.capstone.filenexus.exception.DownloadAccessDeniedException;
import com.capstone.filenexus.exception.FileDownloadException;
import com.capstone.filenexus.service.FileAccessLogService;
import com.capstone.filenexus.service.FileAuthorizationService;
import com.capstone.filenexus.service.FileDownloadResult;
import com.capstone.filenexus.service.FileDownloadService;
import com.capstone.filenexus.service.S3StorageService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileDownloadServiceImpl implements FileDownloadService {

    private final FileAuthorizationService authorizationService;
    private final S3StorageService s3StorageService;
    private final FileAccessLogService accessLogService;

    public FileDownloadServiceImpl(
            FileAuthorizationService authorizationService,
            S3StorageService s3StorageService,
            FileAccessLogService accessLogService
    ) {
        this.authorizationService = authorizationService;
        this.s3StorageService = s3StorageService;
        this.accessLogService = accessLogService;
    }

    @Override
    @Transactional
    public FileDownloadResult downloadFile(
            Long fileId,
            String ipAddress,
            String email,
            UserRole role
    ) {

        // Step 1: Verify file ownership and user permissions.
        // USER: own files only.
        // ADMIN: all files.
        // SECURITY_ANALYST: all files for security review.
        FileEntity file = authorizationService.getFileForRead(
                fileId,
                email,
                role
        );

        // Step 2: Only approved files can be downloaded.
        if (file.getStatus() != FileStatus.APPROVED) {

            String reason = "Download denied. File status is "
                    + file.getStatus();

            accessLogService.recordAccess(
                    fileId,
                    AccessType.DOWNLOAD,
                    AccessResult.DENIED,
                    ipAddress,
                    reason
            );

            throw new DownloadAccessDeniedException(reason);
        }

        // Step 3: Verify clean S3 object key.
        if (file.getCleanKey() == null
                || file.getCleanKey().isBlank()) {

            String reason =
                    "Download denied. Clean object key is missing";

            accessLogService.recordAccess(
                    fileId,
                    AccessType.DOWNLOAD,
                    AccessResult.DENIED,
                    ipAddress,
                    reason
            );

            throw new DownloadAccessDeniedException(reason);
        }

        try {

            // Step 4: Download only from the private clean S3 bucket.
            byte[] fileBytes =
                    s3StorageService.downloadFromClean(
                            file.getCleanKey()
                    );

            if (fileBytes == null || fileBytes.length == 0) {
                throw new IllegalStateException(
                        "Downloaded file is empty"
                );
            }

            // Step 5: Record successful download.
            accessLogService.recordAccess(
                    fileId,
                    AccessType.DOWNLOAD,
                    AccessResult.ALLOWED,
                    ipAddress,
                    null
            );

            // Step 6: Determine response content type.
            String contentType = file.getDetectedMimeType();

            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            // Step 7: Return file content.
            return new FileDownloadResult(
                    fileBytes,
                    file.getOriginalName(),
                    contentType
            );

        } catch (DownloadAccessDeniedException exception) {

            throw exception;

        } catch (Exception exception) {

            // Step 8: Record failed download.
            accessLogService.recordAccess(
                    fileId,
                    AccessType.DOWNLOAD,
                    AccessResult.FAILED,
                    ipAddress,
                    "File download failed: "
                            + exception.getClass().getSimpleName()
            );

            throw new FileDownloadException(
                    "Unable to download the file from storage",
                    exception
            );
        }
    }
}
