
package com.capstone.filenexus.dto.response;

import com.capstone.filenexus.entity.enums.ActorType;

import java.time.Instant;
import java.util.Map;

public class AuditLogResponse {

    private Long auditId;
    private Long userId;
    private Long fileId;
    private ActorType actorType;
    private String action;
    private String previousStatus;
    private String newStatus;
    private Map<String, Object> details;
    private String correlationId;
    private Instant createdAt;

    public AuditLogResponse(
            Long auditId,
            Long userId,
            Long fileId,
            ActorType actorType,
            String action,
            String previousStatus,
            String newStatus,
            Map<String, Object> details,
            String correlationId,
            Instant createdAt
    ) {
        this.auditId = auditId;
        this.userId = userId;
        this.fileId = fileId;
        this.actorType = actorType;
        this.action = action;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.details = details;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    public Long getAuditId() {
        return auditId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getFileId() {
        return fileId;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public String getAction() {
        return action;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
