
package com.capstone.filenexus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ScanFailureRequest {

    @NotBlank(message = "Failure reason is required")
    @Size(max = 2000, message = "Failure reason cannot exceed 2000 characters")
    private String failureReason;

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
