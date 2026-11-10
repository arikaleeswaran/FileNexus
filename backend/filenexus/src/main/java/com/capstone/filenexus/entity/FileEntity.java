package com.capstone.filenexus.entity;

import com.capstone.filenexus.entity.enums.FileStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "quarantine_key", nullable = false,
            unique = true, length = 1024)
    private String quarantineKey;

    @Column(name = "clean_key", unique = true, length = 1024)
    private String cleanKey;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "declared_mime_type", length = 150)
    private String declaredMimeType;

    @Column(name = "detected_mime_type", length = 150)
    private String detectedMimeType;

    @Column(name = "file_extension", length = 20)
    private String fileExtension;

    @Column(name = "sha256_checksum", length = 64)
    private String sha256Checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FileStatus status = FileStatus.UPLOADED;

    @Column(name = "retention_until")
    private Instant retentionUntil;

    @Column(name = "uploaded_at", insertable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "file")
    private List<ScanJob> scanJobs = new ArrayList<>();

    @OneToMany(mappedBy = "file")
    private List<ValidationLog> validationLogs = new ArrayList<>();

    @OneToMany(mappedBy = "file")
    private List<AuditLog> auditLogs = new ArrayList<>();

    @OneToMany(mappedBy = "file")
    private List<FileAccessLog> accessLogs = new ArrayList<>();
}
