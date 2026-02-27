package com.company.hrsystem.holiday.entity;

import com.company.hrsystem.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "holiday",
        indexes = {
                @Index(name = "idx_holiday_company_id", columnList = "company_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_holiday_company_date", columnNames = {"company_id", "holiday_date"})
        })
public class Holiday extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    @Column(name = "name", nullable = false, length = 150)
    private String name;
}
