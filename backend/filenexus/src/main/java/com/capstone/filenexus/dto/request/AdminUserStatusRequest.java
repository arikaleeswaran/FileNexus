package com.capstone.filenexus.dto.request;

import jakarta.validation.constraints.NotNull;

public class AdminUserStatusRequest {
    @NotNull(message = "Active status is required")
    private Boolean active;
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
