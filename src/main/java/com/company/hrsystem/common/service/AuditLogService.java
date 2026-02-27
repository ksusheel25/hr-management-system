package com.company.hrsystem.common.service;

import com.company.hrsystem.common.entity.AuditLog;
import com.company.hrsystem.common.repository.AuditLogRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            UUID tenantId,
            UUID userId,
            String username,
            String action,
            String module,
            UUID entityId,
            String ipAddress) {
        try {
            var auditLog = new AuditLog();
            auditLog.setCompanyId(tenantId);
            auditLog.setTenantId(tenantId);
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setAction(action);
            auditLog.setModule(module);
            auditLog.setEntityId(entityId);
            auditLog.setTimestamp(Instant.now());
            auditLog.setIpAddress(ipAddress);
            auditLogRepository.save(auditLog);
            log.info("business_audit_saved tenant_id={} user_id={} module={} action={} entity_id={}",
                    tenantId, userId, module, action, entityId);
        } catch (Exception ex) {
            log.error("business_audit_save_failed tenant_id={} user_id={} module={} action={} entity_id={}",
                    tenantId, userId, module, action, entityId, ex);
        }
    }
}
