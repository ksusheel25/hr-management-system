package com.company.hrsystem.biometric.entity;

public enum BiometricEventType {
    CHECK_IN,
    CHECK_OUT,
    OFFICE_ENTRY,
    OFFICE_EXIT;

    public static BiometricEventType from(String value) {
        try {
            return BiometricEventType.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unsupported biometric event type: " + value);
        }
    }
}
