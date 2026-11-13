
package com.capstone.filenexus.repository;

import com.capstone.filenexus.entity.ScanJob;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScanJobRepository
        extends JpaRepository<ScanJob, Long> {

    Optional<ScanJob> findByIdempotencyKey(String idempotencyKey);

    Optional<ScanJob> findTopByFileFileIdOrderByAttemptNumberDesc(
            Long fileId
    );

    List<ScanJob> findByFileFileIdOrderByAttemptNumberDesc(
            Long fileId
    );

    boolean existsByIdempotencyKey(String idempotencyKey);
}
