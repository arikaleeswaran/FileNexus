
package com.capstone.filenexus.service;

import com.capstone.filenexus.entity.enums.UserRole;

public interface FileDownloadService {

    FileDownloadResult downloadFile(
            Long fileId,
            String ipAddress,
            String email,
            UserRole role
    );
}
