package com.capstone.filenexus.entity.enums;

public enum FileStatus {
    UPLOADED,
    VALIDATING,
    VALIDATED,
    VALIDATION_FAILED,
    QUARANTINED,
    SCANNING,
    APPROVED,
    REJECTED,
    SCAN_TIMEOUT,
    SCAN_FAILED,
    COPY_FAILED
}