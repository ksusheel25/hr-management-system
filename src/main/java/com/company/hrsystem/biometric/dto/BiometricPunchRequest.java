package com.company.hrsystem.biometric.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BiometricPunchRequest(
        @NotBlank String employeeId,
        @NotNull LocalDateTime timestamp,
        @NotBlank String deviceId) {
}
