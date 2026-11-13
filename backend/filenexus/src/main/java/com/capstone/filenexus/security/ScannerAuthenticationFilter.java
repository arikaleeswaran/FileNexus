
package com.capstone.filenexus.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class ScannerAuthenticationFilter extends OncePerRequestFilter {

    // TEMPORARY DEVELOPMENT SECRET
    private static final String SCANNER_SECRET =
            "FileNexus-Scanner-Dev-Secret-2026";


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();
        String method = request.getMethod();

        // Scanner must read an individual scan job
        boolean scannerRead =
                path.matches("/api/scan-jobs/[0-9]+")
                        && "GET".equalsIgnoreCase(method);

        // Scanner lifecycle operations
        boolean scannerLifecycle =
                path.matches("/api/scan-jobs/[0-9]+/(start|complete|fail|timeout)")
                        && "PATCH".equalsIgnoreCase(method);

        // Internal file release
        boolean internalRelease =
                path.matches("/api/internal/file-release/[0-9]+")
                        && "POST".equalsIgnoreCase(method);

        return !(scannerRead || scannerLifecycle || internalRelease);
    }


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String suppliedSecret = request.getHeader("X-Scanner-Token");

        if (suppliedSecret == null ||
                !MessageDigest.isEqual(
                        SCANNER_SECRET.getBytes(StandardCharsets.UTF_8),
                        suppliedSecret.getBytes(StandardCharsets.UTF_8)
                )) {

            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid scanner credentials"
            );
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "filenexus-scanner",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_SCANNER_SERVICE"
                                )
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
