
package com.capstone.filenexus.dto.request;

import com.capstone.filenexus.entity.enums.ScanResult;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CompleteScanRequest {

    @NotNull(message = "Scan result is required")
    private ScanResult scanResult;

    @Size(max = 50, message = "Scanner version cannot exceed 50 characters")
    private String scannerVersion;

    public ScanResult getScanResult() {
        return scanResult;
    }

    public void setScanResult(ScanResult scanResult) {
        this.scanResult = scanResult;
    }

    public String getScannerVersion() {
        return scannerVersion;
    }

    public void setScannerVersion(String scannerVersion) {
        this.scannerVersion = scannerVersion;
    }
}
