
package com.capstone.filenexus.service;

import com.capstone.filenexus.entity.enums.AccessResult;
import com.capstone.filenexus.entity.enums.AccessType;

public interface FileAccessLogService {

    void recordAccess(
            Long fileId,
            AccessType accessType,
            AccessResult accessResult,
            String ipAddress,
            String denialReason
    );
}
