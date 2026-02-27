package com.company.hrsystem.shift.entity;

import com.company.hrsystem.common.entity.BaseEntity;
import com.company.hrsystem.company.entity.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "shift", indexes = {
        @Index(name = "idx_shift_company_id", columnList = "company_id")
})
public class Shift extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_ref_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false, length = 100)
    private String shiftName;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "grace_minutes")
    private Integer graceMinutes;

    @Column(name = "minimum_half_day_minutes")
    private Integer minimumHalfDayMinutes;

    @Column(name = "minimum_full_day_minutes")
    private Integer minimumFullDayMinutes;
}
