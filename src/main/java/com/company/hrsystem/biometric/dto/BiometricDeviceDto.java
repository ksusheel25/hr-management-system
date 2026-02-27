package com.company.hrsystem.biometric.dto;

import java.util.UUID;

public record BiometricDeviceDto(UUID id, String deviceName, String serialNumber) {
}
