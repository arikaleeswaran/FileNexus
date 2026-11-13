
package com.capstone.filenexus.repository;

import com.capstone.filenexus.entity.FileEntity;
import com.capstone.filenexus.entity.enums.FileStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends
        JpaRepository<FileEntity, Long>,
        JpaSpecificationExecutor<FileEntity> {

    List<FileEntity> findByUserUserId(Long userId);

    List<FileEntity> findByStatus(FileStatus status);

    List<FileEntity> findByOriginalNameContainingIgnoreCase(
            String keyword
    );

    Optional<FileEntity> findBySha256Checksum(String sha256Checksum);

    boolean existsBySha256Checksum(String sha256Checksum);

    long countByStatus(FileStatus status);
}
