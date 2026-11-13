
package com.capstone.filenexus.controller;

import com.capstone.filenexus.dto.response.ApiResponse;
import com.capstone.filenexus.dto.response.AuditLogResponse;
import com.capstone.filenexus.service.AuditLogService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    // Get audit history for a file
    @GetMapping("/files/{fileId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>>
    getFileAuditHistory(@PathVariable Long fileId) {

        List<AuditLogResponse> logs =
                auditLogService.getFileAuditHistory(fileId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "File audit history retrieved successfully",
                        logs
                )
        );
    }

    // Get audit history for a user
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>>
    getUserAuditHistory(@PathVariable Long userId) {

        List<AuditLogResponse> logs =
                auditLogService.getUserAuditHistory(userId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "User audit history retrieved successfully",
                        logs
                )
        );
    }
}
