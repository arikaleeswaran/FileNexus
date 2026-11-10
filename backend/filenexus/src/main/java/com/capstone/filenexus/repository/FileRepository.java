package com.capstone.filenexus.repository;


import com.capstone.filenexus.entity.FileEntity;
import com.capstone.filenexus.entity.enums.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    List<FileEntity> findAllByUser_UserIdOrderByUploadedAtDesc(
            Long userId
    );

    Optional<FileEntity> findByFileIdAndUser_UserId(
            Long fileId,
            Long userId
    );

    List<FileEntity> findAllByStatus(FileStatus status);

    boolean existsBySha256ChecksumAndStatus(
            String checksum,
            FileStatus status
    );
}

