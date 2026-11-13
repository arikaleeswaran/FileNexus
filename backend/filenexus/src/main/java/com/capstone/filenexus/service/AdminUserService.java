package com.capstone.filenexus.service;

import com.capstone.filenexus.dto.response.UserResponse;
import com.capstone.filenexus.entity.enums.UserRole;
import java.util.List;

public interface AdminUserService {
    List<UserResponse> getAllUsers();
    UserResponse updateUserStatus(Long userId, boolean active, String actingAdminEmail);
    UserResponse updateUserRole(Long userId, UserRole role, String actingAdminEmail);
}
