package com.company.hrsystem.leave.entity;

import com.company.hrsystem.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "leave_balance",
        indexes = {
                @Index(name = "idx_leave_balance_company_id", columnList = "company_id"),
                @Index(name = "idx_leave_balance_employee_id", columnList = "employee_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_leave_balance_company_employee_type_year",
                        columnNames = {"company_id", "employee_id", "leave_type_id", "balance_year"})
        })
public class LeaveBalance extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "leave_type_id", nullable = false)
    private UUID leaveTypeId;

    @Column(name = "balance_year", nullable = false)
    private Integer year;

    @Column(name = "allocated", nullable = false)
    private Integer allocated;

    @Column(name = "used", nullable = false)
    private Integer used;

    @Column(name = "remaining", nullable = false)
    private Integer remaining;
}
