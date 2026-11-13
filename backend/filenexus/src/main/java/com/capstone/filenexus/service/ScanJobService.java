
package com.capstone.filenexus.service;

import com.capstone.filenexus.dto.request.CreateScanJobRequest;
import com.capstone.filenexus.dto.request.CompleteScanRequest;
import com.capstone.filenexus.dto.request.ScanFailureRequest;
import com.capstone.filenexus.dto.response.ScanJobResponse;

import java.util.List;

public interface ScanJobService {

    ScanJobResponse createScanJob(
            Long fileId,
            CreateScanJobRequest request
    );

    ScanJobResponse getScanJobById(Long scanId);

    List<ScanJobResponse> getScanHistory(Long fileId);

    ScanJobResponse startScan(Long scanId);

    ScanJobResponse completeScan(
            Long scanId,
            CompleteScanRequest request
    );

    ScanJobResponse failScan(
            Long scanId,
            ScanFailureRequest request
    );

    ScanJobResponse timeoutScan(
            Long scanId,
            ScanFailureRequest request
    );
}
