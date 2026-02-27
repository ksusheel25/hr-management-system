package com.company.hrsystem.biometric.repository;

import com.company.hrsystem.biometric.entity.BiometricEventLog;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BiometricEventLogRepository extends JpaRepository<BiometricEventLog, UUID> {

    boolean existsByDeviceLogIdAndCompanyId(String deviceLogId, UUID companyId);

    Optional<BiometricEventLog> findByIdAndCompanyId(UUID id, UUID companyId);
}
