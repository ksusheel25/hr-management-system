package com.company.hrsystem.biometric.repository;

import com.company.hrsystem.biometric.entity.BiometricDevice;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BiometricDeviceRepository extends JpaRepository<BiometricDevice, UUID> {
}
