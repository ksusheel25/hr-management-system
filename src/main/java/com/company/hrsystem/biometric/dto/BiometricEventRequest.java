package com.company.hrsystem.biometric.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BiometricEventRequest(
        @NotBlank String deviceId,
        @NotBlank String employeeCode,
        @NotBlank String eventType,
        @NotNull LocalDateTime eventTime,
        @NotBlank String deviceLogId) {
}
