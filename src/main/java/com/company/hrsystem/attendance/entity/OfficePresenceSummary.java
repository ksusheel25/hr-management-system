package com.company.hrsystem.attendance.entity;

import com.company.hrsystem.common.entity.BaseEntity;
import com.company.hrsystem.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "office_presence_summary", indexes = {
        @Index(name = "idx_office_presence_summary_company_id", columnList = "company_id")
})
public class OfficePresenceSummary extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "office_entry_time")
    private LocalDateTime officeEntryTime;

    @Column(name = "office_exit_time")
    private LocalDateTime officeExitTime;

    @Column(name = "office_duration_minutes")
    private Long officeDurationMinutes;
}
