package com.capstone.filenexus.entity;

import com.capstone.filenexus.entity.enums.ScanResult;
import com.capstone.filenexus.entity.enums.ScanStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "scan_jobs")
@Getter
@Setter
@NoArgsConstructor
public class ScanJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scan_id")
    private Long scanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "idempotency_key",
            nullable = false, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", nullable = false, length = 30)
    private ScanStatus scanStatus = ScanStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_result", length = 30)
    private ScanResult scanResult;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "scanner_version", length = 50)
    private String scannerVersion;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}

