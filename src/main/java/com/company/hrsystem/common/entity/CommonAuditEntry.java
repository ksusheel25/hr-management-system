package com.company.hrsystem.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "common_audit_entry", indexes = {
        @Index(name = "idx_common_audit_entry_company_id", columnList = "company_id")
})
public class CommonAuditEntry extends BaseEntity {

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", length = 4000)
    private String payload;
}
