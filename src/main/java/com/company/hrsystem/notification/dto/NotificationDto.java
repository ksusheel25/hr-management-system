package com.company.hrsystem.notification.dto;

import com.company.hrsystem.notification.entity.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        String title,
        String message,
        NotificationType type,
        UUID referenceId,
        boolean read,
        Instant createdAt) {
}
