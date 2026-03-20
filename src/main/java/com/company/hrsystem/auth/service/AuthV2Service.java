package com.company.hrsystem.auth.service;

import com.company.hrsystem.auth.dto.v2.V2AuthResponse;
import com.company.hrsystem.auth.dto.v2.V2LoginRequest;
import com.company.hrsystem.auth.dto.v2.V2RefreshRequest;
import com.company.hrsystem.auth.security.CustomUserDetailsService;
import com.company.hrsystem.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthV2Service {

    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Value("${security.jwt.expiration-seconds:3600}")
    private long accessExpirationSeconds;

    @Transactional(readOnly = true)
    public V2AuthResponse login(V2LoginRequest request) {
        var principal = customUserDetailsService.loadByTenantAndUsername(request.tenantId(), request.username());
        if (!passwordEncoder.matches(request.password(), principal.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        var accessToken = jwtTokenProvider.generateToken(principal);
        var refreshToken = refreshTokenService.issue(principal.getTenantId(), principal.getUserId());

        return new V2AuthResponse(
                accessToken,
                refreshToken,
                accessExpirationSeconds,
                new V2AuthResponse.V2User(
                        principal.getUserId(),
                        principal.getEmployeeId(),
                        principal.getUsername(),
                        principal.getRole().name(),
                        principal.getTenantId()));
    }

    @Transactional(readOnly = true)
    public V2AuthResponse refresh(V2RefreshRequest request) {
        var refresh = refreshTokenService.requireValid(request.refreshToken());
        var principal = customUserDetailsService.loadByIdAndTenant(refresh.getUserId(), refresh.getTenantId());
        var accessToken = jwtTokenProvider.generateToken(principal);

        return new V2AuthResponse(
                accessToken,
                request.refreshToken(),
                accessExpirationSeconds,
                new V2AuthResponse.V2User(
                        principal.getUserId(),
                        principal.getEmployeeId(),
                        principal.getUsername(),
                        principal.getRole().name(),
                        principal.getTenantId()));
    }
}

