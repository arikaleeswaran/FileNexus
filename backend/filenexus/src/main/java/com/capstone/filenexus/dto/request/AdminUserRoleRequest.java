package com.capstone.filenexus.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AdminUserRoleRequest {
    @NotBlank(message = "Role is required")
    private String role;
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
