package com.capstone.filenexus.service.impl;

import com.capstone.filenexus.dto.response.UserResponse;
import com.capstone.filenexus.entity.User;
import com.capstone.filenexus.entity.enums.UserRole;
import com.capstone.filenexus.exception.BadRequestException;
import com.capstone.filenexus.exception.ResourceNotFoundException;
import com.capstone.filenexus.repository.UserRepository;
import com.capstone.filenexus.service.AdminUserService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
public class AdminUserServiceImpl implements AdminUserService {
    private final UserRepository userRepository;

    public AdminUserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long userId, boolean active, String actingAdminEmail) {
        User target = findUser(userId);
        boolean self = target.getEmail().equalsIgnoreCase(actingAdminEmail);
        if (self && !active) {
            throw new BadRequestException("You cannot deactivate your own administrator account");
        }
        if (!active) ensureAnotherActiveAdminExists(target);
        target.setActive(active);
        return toResponse(userRepository.save(target));
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(Long userId, UserRole newRole, String actingAdminEmail) {
        User target = findUser(userId);
        boolean self = target.getEmail().equalsIgnoreCase(actingAdminEmail);
        if (self && target.getRole() == UserRole.ADMIN && newRole != UserRole.ADMIN) {
            throw new BadRequestException("You cannot remove your own ADMIN role");
        }
        if (target.isActive() && target.getRole() == UserRole.ADMIN && newRole != UserRole.ADMIN) {
            ensureAnotherActiveAdminExists(target);
        }
        target.setRole(newRole);
        return toResponse(userRepository.save(target));
    }

    private void ensureAnotherActiveAdminExists(User target) {
        if (target.getRole() == UserRole.ADMIN && target.isActive()
                && userRepository.countByRoleAndActiveTrue(UserRole.ADMIN) <= 1) {
            throw new BadRequestException("At least one active administrator must remain");
        }
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getUserId(), user.getUsername(), user.getEmail(),
                user.getRole(), user.isActive(), user.getCreatedAt());
    }
}
