package com.company.hrsystem.biometric.controller;

import com.company.hrsystem.biometric.dto.BiometricEventRequest;
import com.company.hrsystem.biometric.service.BiometricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/biometric")
@PreAuthorize("hasRole('HR')")
@RequiredArgsConstructor
public class BiometricController {

    private final BiometricService biometricService;

    @PostMapping("/events")
    public ResponseEntity<Void> receiveEvent(@Valid @RequestBody BiometricEventRequest request) {
        biometricService.receiveEvent(request);
        return ResponseEntity.ok().build();
    }
}
