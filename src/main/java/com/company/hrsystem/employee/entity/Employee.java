package com.company.hrsystem.employee.entity;

import com.company.hrsystem.common.entity.BaseEntity;
import com.company.hrsystem.company.entity.Company;
import com.company.hrsystem.shift.entity.Shift;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "employee",
        indexes = {
                @Index(name = "idx_employee_company_id", columnList = "company_id"),
                @Index(name = "idx_employee_manager_id", columnList = "manager_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_employee_company_id_employee_code", columnNames = {"company_id", "employee_code"})
        })
public class Employee extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_ref_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "remaining_wfh_balance")
    private Integer remainingWfhBalance;

    @Column(name = "manager_id")
    private java.util.UUID managerId;
}
