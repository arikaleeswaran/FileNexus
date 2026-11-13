package com.capstone.filenexus.service.impl;

import com.capstone.filenexus.dto.response.FileValidationResponse;
import com.capstone.filenexus.dto.response.ValidationCheckResponse;
import com.capstone.filenexus.entity.AuditLog;
import com.capstone.filenexus.entity.FileEntity;
import com.capstone.filenexus.entity.ValidationLog;
import com.capstone.filenexus.entity.enums.ActorType;
import com.capstone.filenexus.entity.enums.FileStatus;
import com.capstone.filenexus.entity.enums.ValidationResult;
import com.capstone.filenexus.exception.BadRequestException;
import com.capstone.filenexus.exception.ResourceNotFoundException;
import com.capstone.filenexus.repository.FileRepository;
import com.capstone.filenexus.repository.ValidationLogRepository;
import com.capstone.filenexus.service.AuditLogService;
import com.capstone.filenexus.service.FileValidationService;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FileValidationServiceImpl implements FileValidationService {

    private final FileRepository fileRepository;
    private final ValidationLogRepository validationLogRepository;
    private final AuditLogService auditLogService;

    private final Tika tika = new Tika();

    private final long maxFileSize;
    private final Set<String> allowedExtensions;

    public FileValidationServiceImpl(
            FileRepository fileRepository,
            ValidationLogRepository validationLogRepository,
            AuditLogService auditLogService,
            @Value("${filenexus.validation.max-file-size:10MB}") org.springframework.util.unit.DataSize maxFileSize,
            @Value("${filenexus.validation.allowed-extensions:pdf,png,jpg,jpeg,docx,txt}") String extensions
    ) {
        this.fileRepository = fileRepository;
        this.validationLogRepository = validationLogRepository;
        this.auditLogService = auditLogService;
        this.maxFileSize = maxFileSize.toBytes();

        Set<String> parsedExtensions = new HashSet<>();

        for (String extension : extensions.split(",")) {
            String normalized = extension.trim().toLowerCase(Locale.ROOT);

            if (!normalized.isBlank()) {
                parsedExtensions.add(normalized);
            }
        }

        this.allowedExtensions = Set.copyOf(parsedExtensions);
    }

    @Override
    @Transactional
    public FileValidationResponse validateFile(
            Long fileId,
            MultipartFile uploadedFile
    ) {

        if (uploadedFile == null || uploadedFile.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "File not found with ID: " + fileId
                        )
                );

        if (file.getStatus() != FileStatus.UPLOADED
                && file.getStatus() != FileStatus.VALIDATION_FAILED) {

            throw new BadRequestException(
                    "File cannot be validated in its current status: "
                            + file.getStatus()
            );
        }

        if (file.getOriginalName() != null
                && !file.getOriginalName().equals(uploadedFile.getOriginalFilename())) {

            throw new BadRequestException(
                    "Uploaded filename does not match the registered file metadata"
            );
        }

        FileStatus previousStatus = file.getStatus();

        file.setStatus(FileStatus.VALIDATING);
        fileRepository.save(file);

        List<ValidationCheckResponse> checks = new ArrayList<>();
        List<ValidationLog> logs = new ArrayList<>();

        String extension = getExtension(uploadedFile.getOriginalFilename());
        String detectedMimeType = null;
        String checksum = null;

        // 1. Extension validation
        boolean extensionValid = validateExtension(extension);

        addCheck(
                file,
                logs,
                checks,
                "EXTENSION",
                extensionValid,
                extensionValid
                        ? "File extension is allowed"
                        : "File extension is not allowed: " + extension
        );


        // 2. File size validation

        System.out.println("========== FILE SIZE DEBUG ==========");
        System.out.println("File ID          : " + fileId);
        System.out.println("Uploaded Size    : " + uploadedFile.getSize());
        System.out.println("Database Size    : " + file.getFileSize());
        System.out.println("Maximum Size     : " + maxFileSize);
        System.out.println("=====================================");

        boolean sizeValid = uploadedFile.getSize() > 0
                && uploadedFile.getSize() <= maxFileSize
                && uploadedFile.getSize() == file.getFileSize();

        String sizeMessage;

        if (uploadedFile.getSize() == 0) {
            sizeMessage = "File is empty";
        } else if (uploadedFile.getSize() > maxFileSize) {
            sizeMessage = "File exceeds the configured maximum size";
        } else if (uploadedFile.getSize() != file.getFileSize()) {
            sizeMessage = "Uploaded file size does not match registered metadata";
        } else {
            sizeMessage = "File size is valid";
        }

        addCheck(
                file,
                logs,
                checks,
                "FILE_SIZE",
                sizeValid,
                sizeMessage
        );

        // 3. MIME type detection
        try {
            detectedMimeType = detectMimeType(uploadedFile, extension);

            boolean mimeValid = isMimeTypeValid(
                    extension,
                    detectedMimeType,
                    file.getDeclaredMimeType()
            );

            addCheck(
                    file,
                    logs,
                    checks,
                    "MIME_TYPE",
                    mimeValid,
                    mimeValid
                            ? "Detected MIME type is consistent with the file format"
                            : "Detected MIME type does not match the extension or declared MIME type"
            );

        } catch (IOException exception) {

            addCheck(
                    file,
                    logs,
                    checks,
                    "MIME_TYPE",
                    false,
                    "Unable to detect MIME type: " + exception.getMessage()
            );
        }

        // 4. File signature validation
        try {
            boolean signatureValid = validateSignature(
                    uploadedFile,
                    extension,
                    detectedMimeType
            );

            addCheck(
                    file,
                    logs,
                    checks,
                    "FILE_SIGNATURE",
                    signatureValid,
                    signatureValid
                            ? "File signature is valid"
                            : "File content does not match the expected file signature"
            );

        } catch (IOException exception) {

            addCheck(
                    file,
                    logs,
                    checks,
                    "FILE_SIGNATURE",
                    false,
                    "Unable to verify file signature: " + exception.getMessage()
            );
        }

        // 5. SHA-256 checksum
        try {
            checksum = calculateSha256(uploadedFile);

            addCheck(
                    file,
                    logs,
                    checks,
                    "SHA256",
                    true,
                    "SHA-256 checksum calculated successfully"
            );

        } catch (IOException | NoSuchAlgorithmException exception) {

            addCheck(
                    file,
                    logs,
                    checks,
                    "SHA256",
                    false,
                    "Unable to calculate SHA-256 checksum"
            );
        }

        boolean valid = checks.stream()
                .allMatch(check ->
                        check.getResult() == ValidationResult.PASSED
                );

        file.setFileExtension(extension);
        file.setDetectedMimeType(detectedMimeType);

        if (checksum != null) {
            file.setSha256Checksum(checksum);
        }

        if (valid) {
            file.setStatus(FileStatus.VALIDATED);
        } else {
            file.setStatus(FileStatus.VALIDATION_FAILED);
        }

        validationLogRepository.saveAll(logs);
        fileRepository.save(file);

        auditLogService.recordEvent(
                null,
                fileId,
                ActorType.SYSTEM,
                "FILE_VALIDATION_COMPLETED",
                previousStatus.name(),
                file.getStatus().name(),
                Map.of(
                        "valid", valid,
                        "detectedMimeType",
                        detectedMimeType == null ? "UNKNOWN" : detectedMimeType,
                        "checksumCalculated", checksum != null
                ),
                UUID.randomUUID().toString()
        );

        return new FileValidationResponse(
                file.getFileId(),
                file.getOriginalName(),
                file.getStatus(),
                file.getDetectedMimeType(),
                file.getSha256Checksum(),
                valid,
                checks
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidationCheckResponse> getValidationHistory(Long fileId) {

        if (!fileRepository.existsById(fileId)) {
            throw new ResourceNotFoundException(
                    "File not found with ID: " + fileId
            );
        }

        return validationLogRepository
                .findByFileFileIdOrderByCreatedAtDesc(fileId)
                .stream()
                .map(log -> new ValidationCheckResponse(
                        log.getValidationType(),
                        log.getValidationResult(),
                        log.getFailureReason() == null
                                ? "Validation passed"
                                : log.getFailureReason()
                ))
                .toList();
    }

    private void addCheck(
            FileEntity file,
            List<ValidationLog> logs,
            List<ValidationCheckResponse> checks,
            String type,
            boolean passed,
            String message
    ) {

        ValidationResult result = passed
                ? ValidationResult.PASSED
                : ValidationResult.FAILED;

        ValidationLog log = new ValidationLog();
        log.setFile(file);
        log.setValidationType(type);
        log.setValidationResult(result);
        log.setFailureReason(passed ? null : message);
        log.setCreatedAt(Instant.now());

        logs.add(log);

        checks.add(
                new ValidationCheckResponse(type, result, message)
        );
    }

    private String getExtension(String filename) {

        if (filename == null || filename.isBlank()) {
            return "";
        }

        int lastDot = filename.lastIndexOf('.');

        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }

        return filename.substring(lastDot + 1)
                .toLowerCase(Locale.ROOT);
    }

    private boolean validateExtension(String extension) {
        return allowedExtensions.contains(extension);
    }

    private String detectMimeType(
            MultipartFile file,
            String extension
    ) throws IOException {

        if ("docx".equals(extension) && isDocxFile(file)) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }

        try (InputStream inputStream = file.getInputStream()) {
            return tika.detect(inputStream, file.getOriginalFilename());
        }
    }

    private boolean isMimeTypeValid(
            String extension,
            String detectedMimeType,
            String declaredMimeType
    ) {

        if (detectedMimeType == null || detectedMimeType.isBlank()) {
            return false;
        }

        String expectedMimeType = getExpectedMimeType(extension);

        if (expectedMimeType == null
                || !expectedMimeType.equalsIgnoreCase(detectedMimeType)) {
            return false;
        }

        if (declaredMimeType == null || declaredMimeType.isBlank()) {
            return true;
        }

        return normalizeMimeType(declaredMimeType)
                .equalsIgnoreCase(detectedMimeType);
    }

    private String normalizeMimeType(String mimeType) {

        String normalized = mimeType.split(";")[0]
                .trim()
                .toLowerCase(Locale.ROOT);

        if ("image/jpg".equals(normalized)) {
            return "image/jpeg";
        }

        return normalized;
    }

    private String getExpectedMimeType(String extension) {

        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "docx" ->
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt" -> "text/plain";
            default -> null;
        };
    }

    private boolean validateSignature(
            MultipartFile file,
            String extension,
            String detectedMimeType
    ) throws IOException {

        if (detectedMimeType == null) {
            return false;
        }

        return switch (extension) {

            case "pdf" ->
                    "application/pdf".equals(detectedMimeType)
                            && startsWith(file, new byte[]{
                            0x25, 0x50, 0x44, 0x46, 0x2D
                    });

            case "png" ->
                    "image/png".equals(detectedMimeType)
                            && startsWith(file, new byte[]{
                            (byte) 0x89, 0x50, 0x4E, 0x47,
                            0x0D, 0x0A, 0x1A, 0x0A
                    });

            case "jpg", "jpeg" ->
                    "image/jpeg".equals(detectedMimeType)
                            && startsWith(file, new byte[]{
                            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
                    });

            case "docx" ->
                    isDocxFile(file)
                            && detectedMimeType.equals(
                            getExpectedMimeType("docx")
                    );

            case "txt" ->
                    "text/plain".equals(detectedMimeType)
                            && isValidTextFile(file);

            default -> false;
        };
    }

    private boolean startsWith(
            MultipartFile file,
            byte[] expected
    ) throws IOException {

        try (InputStream inputStream = file.getInputStream()) {

            byte[] actual = inputStream.readNBytes(expected.length);

            if (actual.length != expected.length) {
                return false;
            }

            for (int i = 0; i < expected.length; i++) {
                if (actual[i] != expected[i]) {
                    return false;
                }
            }

            return true;
        }
    }

    private boolean isDocxFile(MultipartFile file) throws IOException {

        boolean contentTypesFound = false;
        boolean documentFound = false;

        try (ZipInputStream zipInputStream =
                     new ZipInputStream(file.getInputStream())) {

            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {

                if ("[Content_Types].xml".equals(entry.getName())) {
                    contentTypesFound = true;
                }

                if ("word/document.xml".equals(entry.getName())) {
                    documentFound = true;
                }

                if (contentTypesFound && documentFound) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isValidTextFile(MultipartFile file) throws IOException {

        byte[] sample;

        try (InputStream inputStream = file.getInputStream()) {
            sample = inputStream.readNBytes(8192);
        }

        if (sample.length == 0) {
            return false;
        }

        for (byte value : sample) {
            if (value == 0) {
                return false;
            }
        }

        try {
            StandardCharsets.UTF_8.newDecoder()
                    .decode(ByteBuffer.wrap(sample));

            return true;

        } catch (CharacterCodingException exception) {
            return false;
        }
    }

    private String calculateSha256(
            MultipartFile file
    ) throws IOException, NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream inputStream = file.getInputStream();
             DigestInputStream digestInputStream =
                     new DigestInputStream(inputStream, digest)) {

            byte[] buffer = new byte[8192];

            while (digestInputStream.read(buffer) != -1) {
                // DigestInputStream updates the digest as bytes are read.
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }
}