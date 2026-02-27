package com.company.hrsystem.notification.service;

import com.company.hrsystem.auth.security.CustomUserDetails;
import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.notification.dto.MyNotificationsResponse;
import com.company.hrsystem.notification.dto.NotificationDto;
import com.company.hrsystem.notification.entity.NotificationLog;
import com.company.hrsystem.notification.entity.NotificationType;
import com.company.hrsystem.notification.repository.NotificationLogRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationLogRepository notificationLogRepository;

    @Transactional
    public void createNotification(
            UUID tenantId,
            UUID receiverUserId,
            String title,
            String message,
            NotificationType type,
            UUID referenceId) {
        var notification = new NotificationLog();
        notification.setCompanyId(tenantId);
        notification.setTenantId(tenantId);
        notification.setUserId(receiverUserId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setRead(Boolean.FALSE);
        notificationLogRepository.save(notification);
        log.info("notification_created tenant_id={} receiver_user_id={} type={} reference_id={}",
                tenantId, receiverUserId, type, referenceId);
    }

    @Transactional(readOnly = true)
    public MyNotificationsResponse myNotifications() {
        var companyId = requireCompanyId();
        var currentEmployeeId = requireCurrentEmployeeId();

        var notifications = notificationLogRepository.findByCompanyIdAndUserIdOrderByCreatedAtDesc(companyId, currentEmployeeId)
                .stream()
                .map(this::toDto)
                .toList();
        var unreadCount = notificationLogRepository.countByCompanyIdAndUserIdAndReadFalse(companyId, currentEmployeeId);
        return new MyNotificationsResponse(unreadCount, notifications);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        var companyId = requireCompanyId();
        var currentEmployeeId = requireCurrentEmployeeId();

        var notification = notificationLogRepository.findByIdAndCompanyIdAndUserId(notificationId, companyId, currentEmployeeId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        if (!Boolean.TRUE.equals(notification.getRead())) {
            notification.setRead(Boolean.TRUE);
            notificationLogRepository.save(notification);
            log.info("notification_marked_read tenant_id={} user_id={} notification_id={}",
                    companyId, currentEmployeeId, notificationId);
        }
    }

    private NotificationDto toDto(NotificationLog notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getReferenceId(),
                Boolean.TRUE.equals(notification.getRead()),
                notification.getCreatedAt());
    }

    private UUID requireCompanyId() {
        return CompanyContext.getCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context is missing"));
    }

    private UUID requireCurrentEmployeeId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new AccessDeniedException("Authenticated employee context is missing");
        }
        if (principal.getEmployeeId() == null) {
            throw new AccessDeniedException("Authenticated user is not mapped to employee profile");
        }
        return principal.getEmployeeId();
    }
}
