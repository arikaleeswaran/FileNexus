
package com.capstone.filenexus.service;

import com.capstone.filenexus.dto.response.FileUploadResponse;
import com.capstone.filenexus.entity.enums.UserRole;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    FileUploadResponse uploadFile(
            Long fileId,
            MultipartFile file,
            String email,
            UserRole role
    );
}
