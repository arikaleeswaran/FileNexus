package com.capstone.filenexus.controller;

import com.capstone.filenexus.dto.request.AdminUserRoleRequest;
import com.capstone.filenexus.dto.request.AdminUserStatusRequest;
import com.capstone.filenexus.dto.response.UserResponse;
import com.capstone.filenexus.entity.enums.UserRole;
import com.capstone.filenexus.exception.BadRequestException;
import com.capstone.filenexus.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(adminUserService.updateUserStatus(
                userId, request.getActive(), authentication.getName()));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserRoleRequest request,
            Authentication authentication) {
        UserRole role;
        try {
            role = UserRole.valueOf(request.getRole().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Role must be USER, SECURITY_ANALYST, or ADMIN");
        }
        return ResponseEntity.ok(adminUserService.updateUserRole(
                userId, role, authentication.getName()));
    }
}
