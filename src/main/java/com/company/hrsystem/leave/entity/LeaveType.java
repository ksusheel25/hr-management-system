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
@Table(name = "leave_type",
        indexes = {
                @Index(name = "idx_leave_type_company_id", columnList = "company_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_leave_type_company_name", columnNames = {"company_id", "name"})
        })
public class LeaveType extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "annual_quota", nullable = false)
    private Integer annualQuota;

    @Column(name = "carry_forward_allowed", nullable = false)
    private Boolean carryForwardAllowed;
}
