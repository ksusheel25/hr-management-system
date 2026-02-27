package com.company.hrsystem.attendance.entity;

import com.company.hrsystem.common.entity.BaseEntity;
import com.company.hrsystem.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "attendance_event",
        indexes = {
                @Index(name = "idx_attendance_event_company_id", columnList = "company_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_attendance_event_device_log_id", columnNames = {"device_log_id"})
        })
public class AttendanceEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private AttendanceEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private AttendanceSource source;

    @Column(name = "device_log_id", nullable = false, unique = true, length = 120)
    private String deviceLogId;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;
}
