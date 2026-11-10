package com.capstone.filenexus.repository;

import com.capstone.filenexus.entity.ScanJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScanJobRepository extends JpaRepository<ScanJob, Long> {

    List<ScanJob> findAllByFile_FileIdOrderByAttemptNumberDesc(
            Long fileId
    );

    Optional<ScanJob> findByIdempotencyKey(String idempotencyKey);

    boolean existsByFile_FileIdAndAttemptNumber(
            Long fileId,
            Integer attemptNumber
    );
}

