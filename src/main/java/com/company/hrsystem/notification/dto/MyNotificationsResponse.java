package com.company.hrsystem.notification.dto;

import java.util.List;

public record MyNotificationsResponse(
        long unreadCount,
        List<NotificationDto> notifications) {
}
