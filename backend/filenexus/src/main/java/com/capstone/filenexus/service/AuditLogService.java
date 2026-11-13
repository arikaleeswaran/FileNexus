
package com.capstone.filenexus.service;

import com.capstone.filenexus.dto.response.AuditLogResponse;
import com.capstone.filenexus.entity.enums.ActorType;

import java.util.List;
import java.util.Map;

public interface AuditLogService {

    AuditLogResponse recordEvent(
            Long userId,
            Long fileId,
            ActorType actorType,
            String action,
            String previousStatus,
            String newStatus,
            Map<String, Object> details,
            String correlationId
    );

    List<AuditLogResponse> getFileAuditHistory(Long fileId);

    List<AuditLogResponse> getUserAuditHistory(Long userId);
}
