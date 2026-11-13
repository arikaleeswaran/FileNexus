
package com.capstone.filenexus.service;

import com.capstone.filenexus.dto.request.FileMetadataRequest;
import com.capstone.filenexus.dto.response.FileResponse;
import com.capstone.filenexus.dto.response.PageResponse;
import com.capstone.filenexus.entity.enums.FileStatus;
import com.capstone.filenexus.entity.enums.UserRole;

import java.time.Instant;

public interface FileService {

    FileResponse createFileMetadata(
            FileMetadataRequest request,
            String email
    );

    FileResponse getFileById(
            Long fileId,
            String email,
            UserRole role
    );

    PageResponse<FileResponse> getFiles(
            FileStatus status,
            String keyword,
            Instant fromDate,
            Instant toDate,
            int page,
            int size,
            String sortBy,
            String sortDir,
            String email,
            UserRole role
    );

    void deleteFileMetadata(
            Long fileId,
            String email,
            UserRole role
    );
}
