
package com.capstone.filenexus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateScanJobRequest {

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 150, message = "Idempotency key cannot exceed 150 characters")
    private String idempotencyKey;

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
