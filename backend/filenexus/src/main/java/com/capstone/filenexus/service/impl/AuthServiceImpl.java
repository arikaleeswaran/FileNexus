
package com.capstone.filenexus.service.impl;

import com.capstone.filenexus.dto.request.LoginRequest;
import com.capstone.filenexus.dto.response.LoginResponse;
import com.capstone.filenexus.dto.response.UserResponse;
import com.capstone.filenexus.entity.User;
import com.capstone.filenexus.exception.UnauthorizedException;
import com.capstone.filenexus.repository.UserRepository;
import com.capstone.filenexus.security.JwtService;
import com.capstone.filenexus.service.AuthService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 1. Find user by email.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "Invalid email or password"
                        )
                );

        // 2. Check whether the account is active.
        if (!user.isActive()) {
            throw new UnauthorizedException(
                    "Invalid email or password"
            );
        }

        // 3. Verify the submitted password against BCrypt hash.
        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new UnauthorizedException(
                    "Invalid email or password"
            );
        }

        // 4. Generate JWT.
        String token = jwtService.generateToken(user);

        // 5. Build safe user response.
        UserResponse userResponse = new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );

        // 6. Return token and user details.
        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs() / 1000,
                userResponse
        );
    }
}
