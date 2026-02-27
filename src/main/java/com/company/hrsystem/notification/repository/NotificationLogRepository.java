package com.company.hrsystem.notification.repository;

import com.company.hrsystem.notification.entity.NotificationLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findByCompanyIdAndUserIdOrderByCreatedAtDesc(UUID companyId, UUID userId);

    long countByCompanyIdAndUserIdAndReadFalse(UUID companyId, UUID userId);

    Optional<NotificationLog> findByIdAndCompanyIdAndUserId(UUID id, UUID companyId, UUID userId);
}
