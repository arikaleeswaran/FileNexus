package com.capstone.filenexus.entity;

import com.capstone.filenexus.entity.enums.AccessResult;
import com.capstone.filenexus.entity.enums.AccessType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "file_access_logs")
@Getter
@Setter
@NoArgsConstructor
public class FileAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_id")
    private Long accessId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false, length = 30)
    private AccessType accessType;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_result", nullable = false, length = 30)
    private AccessResult accessResult;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "denial_reason")
    private String denialReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}

