package com.company.hrsystem.notification.controller;

import com.company.hrsystem.notification.dto.MyNotificationsResponse;
import com.company.hrsystem.notification.service.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR')")
    public MyNotificationsResponse myNotifications() {
        return notificationService.myNotifications();
    }

    @PutMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR')")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.noContent().build();
    }
}
