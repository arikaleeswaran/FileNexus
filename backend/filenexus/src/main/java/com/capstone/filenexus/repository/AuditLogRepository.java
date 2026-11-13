
package com.capstone.filenexus.repository;

import com.capstone.filenexus.entity.AuditLog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByFileFileIdOrderByCreatedAtDesc(
            Long fileId
    );

    List<AuditLog> findByUserUserIdOrderByCreatedAtDesc(
            Long userId
    );

    List<AuditLog> findByActionOrderByCreatedAtDesc(
            String action
    );
}
