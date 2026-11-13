
package com.capstone.filenexus.service.impl;

import com.capstone.filenexus.entity.FileAccessLog;
import com.capstone.filenexus.entity.FileEntity;
import com.capstone.filenexus.entity.enums.AccessResult;
import com.capstone.filenexus.entity.enums.AccessType;
import com.capstone.filenexus.repository.FileAccessLogRepository;
import com.capstone.filenexus.repository.FileRepository;
import com.capstone.filenexus.service.FileAccessLogService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileAccessLogServiceImpl implements FileAccessLogService {

    private final FileAccessLogRepository accessLogRepository;
    private final FileRepository fileRepository;

    public FileAccessLogServiceImpl(
            FileAccessLogRepository accessLogRepository,
            FileRepository fileRepository
    ) {
        this.accessLogRepository = accessLogRepository;
        this.fileRepository = fileRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAccess(
            Long fileId,
            AccessType accessType,
            AccessResult accessResult,
            String ipAddress,
            String denialReason
    ) {

        FileEntity file = fileRepository.getReferenceById(fileId);

        FileAccessLog log = new FileAccessLog();

        log.setFile(file);

        // Authentication is not implemented yet.
        // user_id is nullable in the current database schema.
        log.setUser(null);

        log.setAccessType(accessType);
        log.setAccessResult(accessResult);
        log.setIpAddress(ipAddress);
        log.setDenialReason(denialReason);

        accessLogRepository.save(log);
    }
}
