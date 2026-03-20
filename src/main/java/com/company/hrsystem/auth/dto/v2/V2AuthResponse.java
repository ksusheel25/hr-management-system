package com.company.hrsystem.auth.dto.v2;

import java.util.UUID;

public record V2AuthResponse(
        String token,
        String refreshToken,
        long expiresIn,
        V2User user) {

    public record V2User(
            UUID id,
            UUID employeeId,
            String username,
            String role,
            UUID tenantId) {
    }
}

