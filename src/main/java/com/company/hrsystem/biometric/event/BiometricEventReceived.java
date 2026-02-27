package com.company.hrsystem.biometric.event;

import java.util.UUID;

public record BiometricEventReceived(UUID biometricEventLogId, UUID companyId) {
}
