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
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "daily_summary",
        indexes = {
                @Index(name = "idx_daily_summary_company_id", columnList = "company_id"),
                @Index(name = "idx_daily_summary_employee_date", columnList = "employee_id, summary_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_daily_summary_company_employee_date",
                        columnNames = {"company_id", "employee_id", "summary_date"})
        })
public class DailySummary extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "summary_date", nullable = false)
    private LocalDate date;

    @Column(name = "worked_minutes", nullable = false)
    private Long totalWorkedMinutes;

    @Column(name = "office_worked_minutes")
    private Long officeWorkedMinutes;

    @Column(name = "office_present")
    private Boolean officePresent;

    @Column(name = "remote_day")
    private Boolean remoteDay;

    @Column(name = "late_minutes")
    private Long lateMinutes;

    @Column(name = "early_exit_minutes")
    private Long earlyExitMinutes;

    @Column(name = "late_arrival")
    private Boolean lateArrival;

    @Column(name = "early_exit")
    private Boolean earlyExit;

    @Column(name = "attendance_status", length = 30)
    @Enumerated(EnumType.STRING)
    private AttendanceStatus attendanceStatus;

    @Column(name = "attendance_mode", length = 20)
    @Enumerated(EnumType.STRING)
    private AttendanceMode mode;

    @Column(name = "finalized")
    private Boolean finalized;

    @Column(name = "overtime_minutes", nullable = false)
    private Integer overtimeMinutes;

    public Long getWorkedMinutes() {
        return totalWorkedMinutes;
    }

    public void setWorkedMinutes(Long workedMinutes) {
        this.totalWorkedMinutes = workedMinutes;
    }

    public AttendanceStatus getStatus() {
        return attendanceStatus;
    }

    public void setStatus(AttendanceStatus status) {
        this.attendanceStatus = status;
    }
}
