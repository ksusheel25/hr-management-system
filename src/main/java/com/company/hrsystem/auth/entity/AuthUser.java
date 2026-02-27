package com.company.hrsystem.auth.entity;

import com.company.hrsystem.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "auth_user",
        indexes = {
                @Index(name = "idx_auth_user_company_id", columnList = "company_id"),
                @Index(name = "idx_auth_user_tenant_id", columnList = "tenant_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_auth_user_tenant_username", columnNames = {"tenant_id", "username"})
        })
public class AuthUser extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "username", nullable = false, length = 120)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String password;

    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Role role;
}
