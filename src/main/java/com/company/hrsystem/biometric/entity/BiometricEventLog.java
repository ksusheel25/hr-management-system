package com.company.hrsystem.biometric.entity;

import com.company.hrsystem.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "biometric_event_log",
        indexes = {
                @Index(name = "idx_biometric_event_log_company_id", columnList = "company_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_biometric_event_log_company_device_log", columnNames = {"company_id", "device_log_id"})
        })
public class BiometricEventLog extends BaseEntity {

    @Column(name = "device_id", nullable = false, length = 120)
    private String deviceId;

    @Column(name = "device_log_id", nullable = false, length = 150)
    private String deviceLogId;

    @Column(name = "employee_code", nullable = false, length = 80)
    private String employeeCode;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "processed", nullable = false)
    private Boolean processed;
}
