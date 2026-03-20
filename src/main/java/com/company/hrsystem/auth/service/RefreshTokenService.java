package com.company.hrsystem.auth.service;

import com.company.hrsystem.auth.entity.RefreshToken;
import com.company.hrsystem.auth.repository.RefreshTokenRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${security.jwt.refresh-expiration-seconds:604800}")
    private long refreshExpirationSeconds;

    @Transactional
    public String issue(UUID tenantId, UUID userId) {
        var token = UUID.randomUUID().toString();
        var entity = new RefreshToken();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setToken(token);
        entity.setExpiresAt(Instant.now().plusSeconds(refreshExpirationSeconds));
        entity.setRevokedAt(null);
        refreshTokenRepository.save(entity);
        return token;
    }

    @Transactional(readOnly = true)
    public RefreshToken requireValid(String refreshToken) {
        var token = refreshTokenRepository.findByTokenAndRevokedAtIsNull(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (Instant.now().isAfter(token.getExpiresAt())) {
            throw new IllegalArgumentException("Refresh token expired");
        }
        return token;
    }

    @Transactional
    public void revoke(String refreshToken) {
        var token = refreshTokenRepository.findByTokenAndRevokedAtIsNull(refreshToken).orElse(null);
        if (token == null) {
            return;
        }
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
    }

    @Transactional
    public long cleanupExpired(Duration retention) {
        var cutoff = Instant.now().minus(retention);
        return refreshTokenRepository.deleteByExpiresAtBefore(cutoff);
    }
}

