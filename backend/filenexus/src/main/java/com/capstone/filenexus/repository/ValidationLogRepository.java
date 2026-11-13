package com.capstone.filenexus.repository;

import com.capstone.filenexus.entity.ValidationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationLogRepository
        extends JpaRepository<ValidationLog, Long> {

    List<ValidationLog> findByFileFileIdOrderByCreatedAtDesc(Long fileId);
}