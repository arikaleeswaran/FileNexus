
package com.capstone.filenexus.config;

import com.capstone.filenexus.entity.User;
import com.capstone.filenexus.entity.enums.UserRole;
import com.capstone.filenexus.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

@Configuration
public class AdminBootstrapConfig {

    @Bean
    public CommandLineRunner provisionInitialAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,

            @Value("${filenexus.bootstrap-admin.enabled:false}")
            boolean enabled,

            @Value("${filenexus.bootstrap-admin.username:}")
            String username,

            @Value("${filenexus.bootstrap-admin.email:}")
            String email,

            @Value("${filenexus.bootstrap-admin.password:}")
            String password
    ) {
        return args -> {

            if (!enabled) {
                return;
            }

            if (username == null || username.isBlank()
                    || email == null || email.isBlank()
                    || password == null || password.length() < 12) {

                throw new IllegalStateException(
                        "Initial admin configuration is incomplete. "
                                + "Username, email and a password of at least "
                                + "12 characters are required."
                );
            }

            String normalizedEmail =
                    email.trim().toLowerCase(Locale.ROOT);

            if (userRepository.existsByEmail(normalizedEmail)) {
                return;
            }

            User admin = new User();

            admin.setUsername(username.trim());
            admin.setEmail(normalizedEmail);
            admin.setPasswordHash(
                    passwordEncoder.encode(password)
            );
            admin.setRole(UserRole.ADMIN);
            admin.setActive(true);

            userRepository.save(admin);
        };
    }
}
