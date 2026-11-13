CREATE TABLE users (
    user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,

    role VARCHAR(30) NOT NULL DEFAULT 'USER'
        CHECK (role IN ('ADMIN', 'USER', 'SECURITY_ANALYST')),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE files (
    file_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL,

    original_name VARCHAR(255) NOT NULL,

    quarantine_key VARCHAR(1024) NOT NULL UNIQUE,
    clean_key VARCHAR(1024) UNIQUE,

    file_size BIGINT NOT NULL CHECK (file_size > 0),

    declared_mime_type VARCHAR(150),
    detected_mime_type VARCHAR(150),

    file_extension VARCHAR(20),

    sha256_checksum CHAR(64),

    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED'
        CHECK (
            status IN (
                'UPLOADED',
                'VALIDATING',
                'QUARANTINED',
                'SCANNING',
                'APPROVED',
                'REJECTED',
                'SCAN_TIMEOUT',
                'SCAN_FAILED',
                'COPY_FAILED'
            )
        ),

    retention_until TIMESTAMPTZ,

    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_files_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_approved_file_has_clean_key
        CHECK (status <> 'APPROVED' OR clean_key IS NOT NULL)
);


CREATE TABLE scan_jobs (
    scan_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    file_id BIGINT NOT NULL,

    attempt_number INT NOT NULL CHECK (attempt_number > 0),

    idempotency_key VARCHAR(150) NOT NULL UNIQUE,

    scan_status VARCHAR(30) NOT NULL DEFAULT 'QUEUED'
        CHECK (
            scan_status IN (
                'QUEUED',
                'RUNNING',
                'COMPLETED',
                'TIMEOUT',
                'FAILED'
            )
        ),

    scan_result VARCHAR(30)
        CHECK (
            scan_result IN ('CLEAN', 'REJECTED')
            OR scan_result IS NULL
        ),

    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    duration_ms BIGINT CHECK (
        duration_ms IS NULL OR duration_ms >= 0
    ),

    failure_reason TEXT,

    scanner_version VARCHAR(50),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_scan_file
        FOREIGN KEY (file_id)
        REFERENCES files(file_id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_file_attempt
        UNIQUE (file_id, attempt_number),

    CONSTRAINT chk_scan_completion
        CHECK (
            scan_status <> 'COMPLETED'
            OR (
                scan_result IS NOT NULL
                AND completed_at IS NOT NULL
            )
        )
);

CREATE TABLE validation_logs (
    validation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    file_id BIGINT NOT NULL,

    validation_type VARCHAR(50) NOT NULL,

    validation_result VARCHAR(20) NOT NULL
        CHECK (
            validation_result IN ('PASSED', 'FAILED')
        ),

    failure_reason TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_validation_file
        FOREIGN KEY (file_id)
        REFERENCES files(file_id)
        ON DELETE RESTRICT
);

CREATE TABLE audit_logs (
    audit_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT,
    file_id BIGINT,

    actor_type VARCHAR(20) NOT NULL DEFAULT 'USER'
        CHECK (actor_type IN ('USER', 'SYSTEM', 'SCANNER')),

    action VARCHAR(100) NOT NULL,

    previous_status VARCHAR(30),
    new_status VARCHAR(30),

    details JSONB,

    correlation_id VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_audit_file
        FOREIGN KEY (file_id)
        REFERENCES files(file_id)
        ON DELETE RESTRICT
);

CREATE TABLE file_access_logs (
    access_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    file_id BIGINT NOT NULL,
    user_id BIGINT,
    ip_address VARCHAR(45),

    access_type VARCHAR(30) NOT NULL
        CHECK (
            access_type IN ('DOWNLOAD', 'VIEW', 'DIRECT_ACCESS')
        ),

    access_result VARCHAR(30) NOT NULL
        CHECK (
            access_result IN (
                'ALLOWED',
                'DENIED',
                'FAILED'
            )
        ),

    ip_address VARCHAR(45),

    denial_reason TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_access_file
        FOREIGN KEY (file_id)
        REFERENCES files(file_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_access_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE RESTRICT
);

-- Users
CREATE INDEX idx_users_role
ON users(role);

-- Files
CREATE INDEX idx_files_user
ON files(user_id);

CREATE INDEX idx_files_status
ON files(status);

CREATE INDEX idx_files_uploaded_at
ON files(uploaded_at);

CREATE INDEX idx_files_checksum
ON files(sha256_checksum);

-- Scan jobs
CREATE INDEX idx_scan_jobs_file
ON scan_jobs(file_id);

CREATE INDEX idx_scan_jobs_status
ON scan_jobs(scan_status);

CREATE INDEX idx_scan_jobs_created
ON scan_jobs(created_at);

-- Validation logs
CREATE INDEX idx_validation_file
ON validation_logs(file_id);

CREATE INDEX idx_validation_result
ON validation_logs(validation_result);

-- Audit logs
CREATE INDEX idx_audit_file
ON audit_logs(file_id);

CREATE INDEX idx_audit_created
ON audit_logs(created_at);

CREATE INDEX idx_audit_action
ON audit_logs(action);

-- File access
CREATE INDEX idx_access_file
ON file_access_logs(file_id);

CREATE INDEX idx_access_user
ON file_access_logs(user_id);

CREATE INDEX idx_access_created
ON file_access_logs(created_at);
