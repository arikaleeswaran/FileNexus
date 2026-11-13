package com.capstone.filenexus.dto.response;

import com.capstone.filenexus.entity.enums.ValidationResult;

public class ValidationCheckResponse {

    private String validationType;
    private ValidationResult result;
    private String message;

    public ValidationCheckResponse(
            String validationType,
            ValidationResult result,
            String message
    ) {
        this.validationType = validationType;
        this.result = result;
        this.message = message;
    }

    public String getValidationType() {
        return validationType;
    }

    public ValidationResult getResult() {
        return result;
    }

    public String getMessage() {
        return message;
    }
}