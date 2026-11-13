
package com.capstone.filenexus.controller;

import com.capstone.filenexus.entity.enums.UserRole;
import com.capstone.filenexus.service.FileDownloadResult;
import com.capstone.filenexus.service.FileDownloadService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
public class FileDownloadController {

    private final FileDownloadService fileDownloadService;

    public FileDownloadController(
            FileDownloadService fileDownloadService
    ) {
        this.fileDownloadService = fileDownloadService;
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Long fileId,
            HttpServletRequest request,
            Authentication authentication
    ) {

        UserRole role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority ->
                        UserRole.valueOf(authority.substring(5)))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("User role is missing"));

        String ipAddress = request.getRemoteAddr();

        FileDownloadResult result = fileDownloadService.downloadFile(
                fileId,
                ipAddress,
                authentication.getName(),
                role
        );

        MediaType mediaType;

        try {
            mediaType = MediaType.parseMediaType(result.contentType());
        } catch (Exception exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(result.originalName(), StandardCharsets.UTF_8)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(result.fileBytes().length);
        headers.setContentDisposition(disposition);
        headers.setCacheControl("no-store");
        headers.set("X-Content-Type-Options", "nosniff");

        return new ResponseEntity<>(
                result.fileBytes(),
                headers,
                HttpStatus.OK
        );
    }
}
