package com.company.hrsystem.attendance.entity;

import com.company.hrsystem.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "work_policy",
        indexes = {
                @Index(name = "idx_work_policy_company_id", columnList = "company_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_work_policy_company_id", columnNames = {"company_id"})
        })
public class WorkPolicy extends BaseEntity {

    public static final int DEFAULT_MINIMUM_WORKING_MINUTES = 480;

    @Column(name = "allowed_wfh_per_month")
    private Integer allowedWfhPerMonth;

    @Column(name = "auto_deduct")
    private Boolean autoDeduct;

    @Column(name = "minimum_working_minutes", nullable = false)
    private Integer minimumWorkingMinutes = DEFAULT_MINIMUM_WORKING_MINUTES;

    @Column(name = "half_day_allowed")
    private Boolean halfDayAllowed;

    @Column(name = "half_day_threshold_minutes")
    private Integer halfDayThresholdMinutes;
}
