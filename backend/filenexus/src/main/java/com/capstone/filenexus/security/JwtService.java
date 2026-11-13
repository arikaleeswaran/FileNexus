
package com.capstone.filenexus.security;

import com.capstone.filenexus.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${filenexus.jwt.secret}") String secret,
            @Value("${filenexus.jwt.expiration-ms}") long expirationMs
    ) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);

            if (keyBytes.length < 32) {
                throw new IllegalArgumentException(
                        "JWT secret must contain at least 32 decoded bytes"
                );
            }

            this.signingKey = Keys.hmacShaKeyFor(keyBytes);

        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Invalid JWT secret. Generate a Base64-encoded secret of at least 32 bytes.",
                    exception
            );
        }

        if (expirationMs <= 0) {
            throw new IllegalArgumentException(
                    "JWT expiration must be greater than zero"
            );
        }

        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {

        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getUserId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {

        try {
            extractClaims(token);
            return true;

        } catch (Exception exception) {
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
