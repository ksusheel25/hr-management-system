package com.company.hrsystem.biometric.entity;

import com.company.hrsystem.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "biometric_device", indexes = {
        @Index(name = "idx_biometric_device_company_id", columnList = "company_id")
})
public class BiometricDevice extends BaseEntity {

    @Column(name = "device_name", nullable = false, length = 120)
    private String deviceName;

    @Column(name = "serial_number", nullable = false, unique = true, length = 120)
    private String serialNumber;
}
