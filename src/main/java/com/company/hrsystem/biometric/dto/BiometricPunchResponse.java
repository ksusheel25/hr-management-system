package com.company.hrsystem.biometric.dto;

import java.time.LocalDateTime;

public record BiometricPunchResponse(
        String employeeId,
        LocalDateTime timestamp,
        String deviceId,
        String punchType,
        String status,
        String message) {
}
