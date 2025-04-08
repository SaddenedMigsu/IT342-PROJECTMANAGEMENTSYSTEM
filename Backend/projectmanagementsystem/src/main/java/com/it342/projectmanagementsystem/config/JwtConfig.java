package com.it342.projectmanagementsystem.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Configuration;

import java.security.Key;

@Configuration
public class JwtConfig {
    private final Key key;
    private final long expiration = 86400000; // 24 hours in milliseconds

    public JwtConfig() {
        // Generate a secure key for HMAC-SHA256
        this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    public Key getKey() {
        return key;
    }

    public long getExpiration() {
        return expiration;
    }
} 