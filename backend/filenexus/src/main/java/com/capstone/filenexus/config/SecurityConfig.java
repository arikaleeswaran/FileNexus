
package com.capstone.filenexus.config;

import com.capstone.filenexus.security.JwtAuthenticationFilter;
import com.capstone.filenexus.security.ScannerAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ScannerAuthenticationFilter scannerAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ScannerAuthenticationFilter scannerAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.scannerAuthenticationFilter = scannerAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                )
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Public authentication
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()

                        .requestMatchers(
                                "/actuator/health"
                        ).permitAll()

                        // Scanner-only lifecycle operations
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/scan-jobs/*/start",
                                "/api/scan-jobs/*/complete",
                                "/api/scan-jobs/*/fail",
                                "/api/scan-jobs/*/timeout"
                        ).hasAuthority("ROLE_SCANNER_SERVICE")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/internal/file-release/**"
                        ).hasAuthority("ROLE_SCANNER_SERVICE")

                        // Scan-job creation and read operations
                                // Scanner can read individual scan jobs
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/scan-jobs/*"
                                ).hasAnyAuthority(
                                        "ROLE_SCANNER_SERVICE",
                                        "ROLE_ADMIN",
                                        "ROLE_SECURITY_ANALYST"
                                )

// Scanner lifecycle operations
                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/scan-jobs/*/start",
                                        "/api/scan-jobs/*/complete",
                                        "/api/scan-jobs/*/fail",
                                        "/api/scan-jobs/*/timeout"
                                ).hasAuthority("ROLE_SCANNER_SERVICE")

// Other scan-job operations
                                .requestMatchers("/api/scan-jobs/**")
                                .hasAnyRole("ADMIN", "SECURITY_ANALYST")

                        // Audit logs
                        .requestMatchers(
                                "/api/audit-logs/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "SECURITY_ANALYST"
                        )

                        // File upload
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/files/**"
                        ).hasAnyRole("ADMIN", "USER")

                        // File viewing and downloading
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/files/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "USER",
                                "SECURITY_ANALYST"
                        )

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        scannerAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
