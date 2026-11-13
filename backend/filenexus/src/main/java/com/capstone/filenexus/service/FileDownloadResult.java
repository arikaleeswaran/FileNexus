
package com.capstone.filenexus.service;

public record FileDownloadResult(
        byte[] fileBytes,
        String originalName,
        String contentType
) {
}
