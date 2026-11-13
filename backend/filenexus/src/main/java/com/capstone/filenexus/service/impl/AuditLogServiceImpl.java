
package com.capstone.filenexus.service.impl;

import com.capstone.filenexus.dto.response.AuditLogResponse;
import com.capstone.filenexus.entity.AuditLog;
import com.capstone.filenexus.entity.FileEntity;
import com.capstone.filenexus.entity.User;
import com.capstone.filenexus.entity.enums.ActorType;
import com.capstone.filenexus.exception.ResourceNotFoundException;
import com.capstone.filenexus.repository.AuditLogRepository;
import com.capstone.filenexus.repository.FileRepository;
import com.capstone.filenexus.repository.UserRepository;
import com.capstone.filenexus.service.AuditLogService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    public AuditLogServiceImpl(
            AuditLogRepository auditLogRepository,
            FileRepository fileRepository,
            UserRepository userRepository
    ) {
        this.auditLogRepository = auditLogRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
    }

    // 1. Record audit event
    @Override
    public AuditLogResponse recordEvent(
            Long userId,
            Long fileId,
            ActorType actorType,
            String action,
            String previousStatus,
            String newStatus,
            Map<String, Object> details,
            String correlationId
    ) {

        AuditLog auditLog = new AuditLog();

        if (userId != null) {

            User user = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "User not found with ID: " + userId
                            )
                    );

            auditLog.setUser(user);
        }

        if (fileId != null) {

            FileEntity file = fileRepository.findById(fileId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "File not found with ID: " + fileId
                            )
                    );

            auditLog.setFile(file);
        }

        auditLog.setActorType(actorType);
        auditLog.setAction(action);
        auditLog.setPreviousStatus(previousStatus);
        auditLog.setNewStatus(newStatus);
        auditLog.setDetails(details);
        auditLog.setCorrelationId(correlationId);

        AuditLog savedLog = auditLogRepository.save(auditLog);

        return mapToResponse(savedLog);
    }

    // 2. Retrieve file audit history
    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getFileAuditHistory(Long fileId) {

        if (!fileRepository.existsById(fileId)) {
            throw new ResourceNotFoundException(
                    "File not found with ID: " + fileId
            );
        }

        return auditLogRepository
                .findByFileFileIdOrderByCreatedAtDesc(fileId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // 3. Retrieve user audit history
    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getUserAuditHistory(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException(
                    "User not found with ID: " + userId
            );
        }

        return auditLogRepository
                .findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // 4. Convert entity into response DTO
    private AuditLogResponse mapToResponse(AuditLog auditLog) {

        return new AuditLogResponse(
                auditLog.getAuditId(),
                auditLog.getUser() != null
                        ? auditLog.getUser().getUserId()
                        : null,
                auditLog.getFile() != null
                        ? auditLog.getFile().getFileId()
                        : null,
                auditLog.getActorType(),
                auditLog.getAction(),
                auditLog.getPreviousStatus(),
                auditLog.getNewStatus(),
                auditLog.getDetails(),
                auditLog.getCorrelationId(),
                auditLog.getCreatedAt()
        );
    }
}
