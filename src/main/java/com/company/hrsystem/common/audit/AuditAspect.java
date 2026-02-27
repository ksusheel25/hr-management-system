package com.company.hrsystem.common.audit;

import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.common.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogService auditLogService;

    @Around("@annotation(auditable)")
    public Object aroundAuditable(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            log.error("auditable_method_failed action={} module={} method={}",
                    auditable.action(), auditable.module(), joinPoint.getSignature().toShortString(), ex);
            throw ex;
        }

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return result;
        }
        var principal = authentication.getPrincipal();
        if (principal == null) {
            return result;
        }

        var tenantId = extractUuid(principal, "getTenantId");
        if (tenantId == null) {
            tenantId = CompanyContext.getCompanyId().orElse(null);
        }
        if (tenantId == null) {
            log.error("auditable_skip_missing_tenant action={} module={} method={}",
                    auditable.action(), auditable.module(), joinPoint.getSignature().toShortString());
            return result;
        }

        var userId = extractUuid(principal, "getUserId");
        var username = extractString(principal, "getUsername");
        if (userId == null || username == null || username.isBlank()) {
            return result;
        }

        var entityId = resolveEntityId(result, joinPoint.getArgs());
        var ipAddress = resolveClientIpAddress();
        auditLogService.record(
                tenantId,
                userId,
                username,
                auditable.action(),
                auditable.module(),
                entityId,
                ipAddress);
        return result;
    }

    private UUID resolveEntityId(Object result, Object[] args) {
        var fromResult = resolveFromResult(result);
        if (fromResult != null) {
            return fromResult;
        }

        for (var arg : args) {
            if (arg instanceof UUID id) {
                return id;
            }
        }
        return null;
    }

    private UUID resolveFromResult(Object result) {
        if (result == null) {
            return null;
        }
        try {
            var getId = result.getClass().getMethod("getId");
            var value = getId.invoke(result);
            if (value instanceof UUID id) {
                return id;
            }
        } catch (Exception ignored) {
            // Ignore and continue with other extraction paths.
        }
        try {
            var idAccessor = result.getClass().getMethod("id");
            var value = idAccessor.invoke(result);
            if (value instanceof UUID id) {
                return id;
            }
        } catch (Exception ignored) {
            // Ignore if no record accessor.
        }
        return null;
    }

    private String resolveClientIpAddress() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        var forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            var first = forwardedFor.split(",")[0].trim();
            if (!first.isBlank()) {
                return first;
            }
        }

        var realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private UUID extractUuid(Object principal, String methodName) {
        try {
            var method = principal.getClass().getMethod(methodName);
            var value = method.invoke(principal);
            return value instanceof UUID uuid ? uuid : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractString(Object principal, String methodName) {
        try {
            var method = principal.getClass().getMethod(methodName);
            var value = method.invoke(principal);
            return value == null ? null : value.toString();
        } catch (Exception ignored) {
            return null;
        }
    }
}
