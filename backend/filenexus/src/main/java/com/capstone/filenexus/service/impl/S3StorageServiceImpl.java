
package com.capstone.filenexus.service.impl;

import com.capstone.filenexus.exception.BadRequestException;
import com.capstone.filenexus.service.S3StorageService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class S3StorageServiceImpl implements S3StorageService {

    private final S3Client s3Client;
    private final String quarantineBucket;
    private final String cleanBucket;

    public S3StorageServiceImpl(
            S3Client s3Client,
            @Value("${filenexus.aws.s3.quarantine-bucket}")
            String quarantineBucket,
            @Value("${filenexus.aws.s3.clean-bucket}")
            String cleanBucket
    ) {
        this.s3Client = s3Client;
        this.quarantineBucket = quarantineBucket;
        this.cleanBucket = cleanBucket;
    }

    @Override
    public void uploadToQuarantine(
            MultipartFile file,
            String objectKey,
            Long fileId,
            String sha256Checksum,
            String contentType
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Cannot upload an empty file");
        }

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(quarantineBucket)
                    .key(objectKey)
                    .contentLength(file.getSize())
                    .contentType(
                            contentType == null
                                    ? "application/octet-stream"
                                    : contentType
                    )
                    .metadata(Map.of(
                            "file-id", fileId.toString(),
                            "sha256", sha256Checksum
                    ))
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .build();

            try (var inputStream = file.getInputStream()) {
                s3Client.putObject(
                        request,
                        RequestBody.fromInputStream(
                                inputStream,
                                file.getSize()
                        )
                );
            }

            HeadObjectResponse headResponse = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(quarantineBucket)
                            .key(objectKey)
                            .build()
            );

            boolean sizeMatches =
                    headResponse.contentLength() != null
                            && headResponse.contentLength() == file.getSize();

            boolean fileIdMatches =
                    fileId.toString().equals(
                            headResponse.metadata().get("file-id")
                    );

            boolean checksumMatches =
                    sha256Checksum.equalsIgnoreCase(
                            headResponse.metadata().get("sha256")
                    );

            if (!sizeMatches || !fileIdMatches || !checksumMatches) {
                deleteQuarantineObject(objectKey);
                throw new IllegalStateException(
                        "S3 quarantine object verification failed"
                );
            }

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read uploaded file",
                    exception
            );
        }
    }

    @Override
    public void deleteQuarantineObject(String objectKey) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(quarantineBucket)
                        .key(objectKey)
                        .build()
        );
    }

    @Override
    public void copyQuarantineToClean(
            String quarantineKey,
            String cleanKey,
            Long fileId,
            Long expectedFileSize,
            String expectedSha256
    ) {

        HeadObjectResponse source = s3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(quarantineBucket)
                        .key(quarantineKey)
                        .build()
        );

        verifyObject(
                source,
                fileId,
                expectedFileSize,
                expectedSha256,
                "Quarantine source"
        );

        String encodedKey = URLEncoder.encode(
                quarantineKey,
                StandardCharsets.UTF_8
        ).replace("+", "%20").replace("%2F", "/");

        String copySource = quarantineBucket + "/" + encodedKey;

        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .copySource(copySource)
                .destinationBucket(cleanBucket)
                .destinationKey(cleanKey)
                .metadataDirective(MetadataDirective.COPY)
                .serverSideEncryption(ServerSideEncryption.AES256)
                .build();

        s3Client.copyObject(copyRequest);

        HeadObjectResponse destination = s3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(cleanBucket)
                        .key(cleanKey)
                        .build()
        );

        verifyObject(
                destination,
                fileId,
                expectedFileSize,
                expectedSha256,
                "Clean destination"
        );
    }

    @Override
    public byte[] downloadFromClean(String cleanKey) {

        if (!StringUtils.hasText(cleanKey)) {
            throw new IllegalArgumentException(
                    "Clean object key is missing"
            );
        }

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(cleanBucket)
                .key(cleanKey)
                .build();

        ResponseBytes<GetObjectResponse> response =
                s3Client.getObjectAsBytes(request);

        return response.asByteArray();
    }

    private void verifyObject(
            HeadObjectResponse response,
            Long fileId,
            Long expectedFileSize,
            String expectedSha256,
            String description
    ) {

        boolean sizeMatches =
                response.contentLength() != null
                        && response.contentLength().equals(expectedFileSize);

        boolean fileIdMatches =
                fileId.toString().equals(
                        response.metadata().get("file-id")
                );

        boolean checksumMatches =
                expectedSha256.equalsIgnoreCase(
                        response.metadata().get("sha256")
                );

        if (!sizeMatches || !fileIdMatches || !checksumMatches) {
            throw new IllegalStateException(
                    description + " verification failed"
            );
        }
    }
}
