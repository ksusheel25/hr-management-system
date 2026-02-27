package com.company.hrsystem.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_log_company_id", columnList = "company_id"),
        @Index(name = "idx_audit_log_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_audit_log_user_id", columnList = "user_id"),
        @Index(name = "idx_audit_log_timestamp", columnList = "audit_timestamp")
})
public class AuditLog extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "username", nullable = false, length = 120, updatable = false)
    private String username;

    @Column(name = "action", nullable = false, length = 120, updatable = false)
    private String action;

    @Column(name = "module", nullable = false, length = 120, updatable = false)
    private String module;

    @Column(name = "entity_id", updatable = false)
    private UUID entityId;

    @Column(name = "audit_timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "ip_address", length = 64, updatable = false)
    private String ipAddress;
}
