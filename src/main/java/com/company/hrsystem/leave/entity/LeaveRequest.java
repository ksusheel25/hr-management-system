package com.company.hrsystem.leave.entity;

import com.company.hrsystem.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "leave_request", indexes = {
        @Index(name = "idx_leave_request_company_id", columnList = "company_id"),
        @Index(name = "idx_leave_request_employee_id", columnList = "employee_id"),
        @Index(name = "idx_leave_request_approver_id", columnList = "approver_id"),
        @Index(name = "idx_leave_request_status", columnList = "status")
})
public class LeaveRequest extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "leave_type", nullable = false, length = 80)
    private String leaveType;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeaveStatus status;

    @Column(name = "approver_id")
    private UUID approverId;

    @Column(name = "remarks", length = 1000)
    private String remarks;
}
