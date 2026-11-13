
package com.capstone.filenexus.dto.response;

import com.capstone.filenexus.entity.enums.UserRole;

import java.time.Instant;

public class UserResponse {

    private Long userId;
    private String username;
    private String email;
    private UserRole role;
    private boolean active;
    private Instant createdAt;

    public UserResponse(
            Long userId,
            String username,
            String email,
            UserRole role,
            boolean active,
            Instant createdAt
    ) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
