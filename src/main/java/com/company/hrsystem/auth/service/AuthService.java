package com.company.hrsystem.auth.service;

import com.company.hrsystem.auth.dto.AuthResponse;
import com.company.hrsystem.auth.dto.LoginRequest;
import com.company.hrsystem.auth.security.CustomUserDetailsService;
import com.company.hrsystem.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        var principal = customUserDetailsService.loadByTenantAndUsername(request.tenantId(), request.username());
        if (!passwordEncoder.matches(request.password(), principal.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        var accessToken = jwtTokenProvider.generateToken(principal);
        return new AuthResponse(accessToken, principal.getRole().name());
    }
}
