package com.company.hrsystem.auth.controller;

import com.company.hrsystem.auth.dto.v2.V2AuthResponse;
import com.company.hrsystem.auth.dto.v2.V2LoginRequest;
import com.company.hrsystem.auth.dto.v2.V2LogoutRequest;
import com.company.hrsystem.auth.dto.v2.V2RefreshRequest;
import com.company.hrsystem.auth.service.AuthV2Service;
import com.company.hrsystem.auth.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/auth")
@RequiredArgsConstructor
public class AuthV2Controller {

    private final AuthV2Service authV2Service;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public V2AuthResponse login(@Valid @RequestBody V2LoginRequest request) {
        return authV2Service.login(request);
    }

    @PostMapping("/refresh")
    public V2AuthResponse refresh(@Valid @RequestBody V2RefreshRequest request) {
        return authV2Service.refresh(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody V2LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        return ResponseEntity.ok().build();
    }
}

