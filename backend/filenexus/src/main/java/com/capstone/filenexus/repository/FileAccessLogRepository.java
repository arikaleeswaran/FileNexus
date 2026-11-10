package com.capstone.filenexus.repository;


import com.capstone.filenexus.entity.FileAccessLog;
import com.capstone.filenexus.entity.enums.AccessResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileAccessLogRepository
        extends JpaRepository<FileAccessLog, Long> {

    List<FileAccessLog> findAllByFile_FileIdOrderByCreatedAtDesc(
            Long fileId
    );

    List<FileAccessLog> findAllByAccessResult(
            AccessResult accessResult
    );
}
