package com.company.hrsystem.common.repository;

import com.company.hrsystem.common.entity.CommonAuditEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonAuditEntryRepository extends JpaRepository<CommonAuditEntry, UUID> {
}
