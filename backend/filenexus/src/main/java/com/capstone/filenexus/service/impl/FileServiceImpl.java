
package com.capstone.filenexus.service.impl;

import com.capstone.filenexus.dto.request.FileMetadataRequest;
import com.capstone.filenexus.dto.response.FileResponse;
import com.capstone.filenexus.dto.response.PageResponse;
import com.capstone.filenexus.entity.FileEntity;
import com.capstone.filenexus.entity.User;
import com.capstone.filenexus.entity.enums.ActorType;
import com.capstone.filenexus.entity.enums.FileStatus;
import com.capstone.filenexus.entity.enums.UserRole;
import com.capstone.filenexus.exception.BadRequestException;
import com.capstone.filenexus.exception.ResourceNotFoundException;
import com.capstone.filenexus.repository.FileRepository;
import com.capstone.filenexus.repository.UserRepository;
import com.capstone.filenexus.service.AuditLogService;
import com.capstone.filenexus.service.FileService;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public FileServiceImpl(
            FileRepository fileRepository,
            UserRepository userRepository,
            AuditLogService auditLogService
    ) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    // Create file metadata for the authenticated user.
    @Override
    public FileResponse createFileMetadata(
            FileMetadataRequest request,
            String email
    ) {

        User user = getAuthenticatedUser(email);

        if (!user.isActive()) {
            throw new BadRequestException(
                    "Inactive users cannot upload files"
            );
        }

        FileEntity file = new FileEntity();

        file.setUser(user);
        file.setOriginalName(request.getOriginalName());
        file.setFileSize(request.getFileSize());
        file.setDeclaredMimeType(request.getDeclaredMimeType());

        // Temporary key until the actual quarantine upload.
        file.setQuarantineKey(
                "pending/" + UUID.randomUUID()
        );

        file.setStatus(FileStatus.UPLOADED);

        FileEntity savedFile = fileRepository.save(file);

        auditLogService.recordEvent(
                user.getUserId(),
                savedFile.getFileId(),
                ActorType.USER,
                "FILE_METADATA_CREATED",
                null,
                FileStatus.UPLOADED.name(),
                Map.of(
                        "fileName", savedFile.getOriginalName(),
                        "fileSize", savedFile.getFileSize(),
                        "message", "File metadata registered successfully"
                ),
                UUID.randomUUID().toString()
        );

        return mapToResponse(savedFile);
    }

    // Retrieve a file only when the caller has permission.
    @Override
    @Transactional(readOnly = true)
    public FileResponse getFileById(
            Long fileId,
            String email,
            UserRole role
    ) {

        FileEntity file = findAccessibleFile(
                fileId,
                email,
                role
        );

        return mapToResponse(file);
    }

    // Retrieve files with pagination, filtering and ownership restrictions.
    @Override
    @Transactional(readOnly = true)
    public PageResponse<FileResponse> getFiles(
            FileStatus status,
            String keyword,
            Instant fromDate,
            Instant toDate,
            int page,
            int size,
            String sortBy,
            String sortDir,
            String email,
            UserRole role
    ) {

        if (page < 0) {
            throw new BadRequestException(
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new BadRequestException(
                    "Page size must be between 1 and 100"
            );
        }

        if (fromDate != null && toDate != null
                && fromDate.isAfter(toDate)) {
            throw new BadRequestException(
                    "From date cannot be after to date"
            );
        }

        Set<String> allowedSortFields = Set.of(
                "fileId",
                "originalName",
                "fileSize",
                "uploadedAt",
                "status"
        );

        if (sortBy == null ||
                !allowedSortFields.contains(sortBy)) {
            throw new BadRequestException(
                    "Invalid sorting field: " + sortBy
            );
        }

        Sort.Direction direction;

        try {
            direction = Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BadRequestException(
                    "Sort direction must be ASC or DESC"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortBy)
        );

        String searchKeyword =
                (keyword == null || keyword.isBlank())
                        ? null
                        : keyword.trim().toLowerCase(Locale.ROOT);

        User currentUser = getAuthenticatedUser(email);

        Specification<FileEntity> specification =
                (root, query, criteriaBuilder) -> {

                    List<Predicate> predicates = new ArrayList<>();

                    // Normal users can only see their own files.
                    if (role == UserRole.USER) {
                        predicates.add(
                                criteriaBuilder.equal(
                                        root.get("user").get("userId"),
                                        currentUser.getUserId()
                                )
                        );
                    }

                    // ADMIN and SECURITY_ANALYST can view all files.
                    if (status != null) {
                        predicates.add(
                                criteriaBuilder.equal(
                                        root.get("status"),
                                        status
                                )
                        );
                    }

                    if (searchKeyword != null) {
                        predicates.add(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get("originalName")
                                        ),
                                        "%" + searchKeyword + "%"
                                )
                        );
                    }

                    if (fromDate != null) {
                        predicates.add(
                                criteriaBuilder.greaterThanOrEqualTo(
                                        root.<Instant>get("uploadedAt"),
                                        fromDate
                                )
                        );
                    }

                    if (toDate != null) {
                        predicates.add(
                                criteriaBuilder.lessThanOrEqualTo(
                                        root.<Instant>get("uploadedAt"),
                                        toDate
                                )
                        );
                    }

                    return criteriaBuilder.and(
                            predicates.toArray(new Predicate[0])
                    );
                };

        Page<FileEntity> filePage =
                fileRepository.findAll(specification, pageable);

        List<FileResponse> files = filePage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                files,
                filePage.getNumber(),
                filePage.getSize(),
                filePage.getTotalElements(),
                filePage.getTotalPages(),
                filePage.isFirst(),
                filePage.isLast()
        );
    }

    // File deletion is not implemented yet.
    @Override
    public void deleteFileMetadata(
            Long fileId,
            String email,
            UserRole role
    ) {

        findAccessibleFile(fileId, email, role);

        throw new BadRequestException(
                "File deletion is not available yet"
        );
    }

    // Common ownership and authorization check.
    private FileEntity findAccessibleFile(
            Long fileId,
            String email,
            UserRole role
    ) {

        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "File not found with ID: " + fileId
                        )
                );

        if (role == UserRole.ADMIN ||
                role == UserRole.SECURITY_ANALYST) {
            return file;
        }

        User user = getAuthenticatedUser(email);

        if (!file.getUser().getUserId().equals(user.getUserId())) {
            // Return 404 instead of revealing another user's file.
            throw new ResourceNotFoundException(
                    "File not found with ID: " + fileId
            );
        }

        return file;
    }

    private User getAuthenticatedUser(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"
                        )
                );
    }

    private FileResponse mapToResponse(FileEntity file) {

        return new FileResponse(
                file.getFileId(),
                file.getOriginalName(),
                file.getFileSize(),
                file.getDeclaredMimeType(),
                file.getDetectedMimeType(),
                file.getSha256Checksum(),
                file.getStatus(),
                file.getUploadedAt()
        );
    }
}
