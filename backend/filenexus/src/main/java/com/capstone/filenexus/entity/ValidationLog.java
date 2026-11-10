package com.capstone.filenexus.entity;

import com.capstone.filenexus.entity.enums.ValidationResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "validation_logs")
@Getter
@Setter
@NoArgsConstructor
public class ValidationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "validation_id")
    private Long validationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @Column(name = "validation_type",
            nullable = false, length = 50)
    private String validationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_result",
            nullable = false, length = 20)
    private ValidationResult validationResult;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}

