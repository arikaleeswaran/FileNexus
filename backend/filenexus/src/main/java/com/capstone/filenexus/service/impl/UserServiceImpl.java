
package com.capstone.filenexus.service.impl;

import com.capstone.filenexus.dto.request.RegisterRequest;
import com.capstone.filenexus.dto.response.UserResponse;
import com.capstone.filenexus.entity.User;
import com.capstone.filenexus.entity.enums.UserRole;
import com.capstone.filenexus.exception.BadRequestException;
import com.capstone.filenexus.repository.UserRepository;
import com.capstone.filenexus.service.UserService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponse registerUser(RegisterRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        String username = request.getUsername().trim();

        // 1. Check whether email already exists.
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException(
                    "An account with this email already exists"
            );
        }

        // 2. Create a new user.
        User user = new User();

        user.setUsername(username);
        user.setEmail(email);

        // 3. Hash the password using BCrypt.
        String encodedPassword = passwordEncoder.encode(
                request.getPassword()
        );

        user.setPasswordHash(encodedPassword);

        // 4. Public registration always assigns USER.
        user.setRole(UserRole.USER);

        user.setActive(true);

        // 5. Save the user.
        User savedUser = userRepository.save(user);

        // 6. Return safe user details.
        return mapToResponse(savedUser);
    }

    private UserResponse mapToResponse(User user) {

        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
