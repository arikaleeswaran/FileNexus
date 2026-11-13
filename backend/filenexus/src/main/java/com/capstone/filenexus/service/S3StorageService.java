
package com.capstone.filenexus.service;

import org.springframework.web.multipart.MultipartFile;

public interface S3StorageService {

    void uploadToQuarantine(
            MultipartFile file,
            String objectKey,
            Long fileId,
            String sha256Checksum,
            String contentType
    );

    void deleteQuarantineObject(String objectKey);

    void copyQuarantineToClean(
            String quarantineKey,
            String cleanKey,
            Long fileId,
            Long expectedFileSize,
            String expectedSha256
    );

    byte[] downloadFromClean(String cleanKey);
}
