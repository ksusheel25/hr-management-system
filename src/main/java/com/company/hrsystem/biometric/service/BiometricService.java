package com.company.hrsystem.biometric.service;

import com.company.hrsystem.attendance.service.AttendanceService;
import com.company.hrsystem.biometric.dto.BiometricEventRequest;
import com.company.hrsystem.biometric.dto.BiometricPunchRequest;
import com.company.hrsystem.biometric.dto.BiometricPunchResponse;
import com.company.hrsystem.biometric.entity.BiometricEventLog;
import com.company.hrsystem.biometric.event.BiometricEventReceived;
import com.company.hrsystem.biometric.exception.EmployeeNotFoundException;
import com.company.hrsystem.biometric.exception.InvalidPunchException;
import com.company.hrsystem.biometric.repository.BiometricEmployeeLookupRepository;
import com.company.hrsystem.biometric.repository.BiometricEventLogRepository;
import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.common.entity.CommonAuditEntry;
import com.company.hrsystem.common.repository.CommonAuditEntryRepository;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class BiometricService {

    private static final Logger log = LoggerFactory.getLogger(BiometricService.class);

    private final BiometricEventLogRepository biometricEventLogRepository;
    private final BiometricEmployeeLookupRepository biometricEmployeeLookupRepository;
    private final AttendanceService attendanceService;
    private final CommonAuditEntryRepository commonAuditEntryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void receiveEvent(BiometricEventRequest request) {
        var companyId = CompanyContext.getCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context is missing"));

        if (biometricEventLogRepository.existsByDeviceLogIdAndCompanyId(request.deviceLogId(), companyId)) {
            throw new IllegalStateException("Duplicate biometric event for deviceLogId");
        }

        var eventLog = new BiometricEventLog();
        eventLog.setCompanyId(companyId);
        eventLog.setDeviceId(request.deviceId());
        eventLog.setDeviceLogId(request.deviceLogId());
        eventLog.setEmployeeCode(request.employeeCode());
        eventLog.setEventType(request.eventType());
        eventLog.setEventTime(request.eventTime());
        eventLog.setProcessed(Boolean.FALSE);

        var savedEventLog = biometricEventLogRepository.save(eventLog);
        publishAfterCommit(new BiometricEventReceived(savedEventLog.getId(), companyId));
    }

    @Transactional
    public BiometricPunchResponse processPunch(BiometricPunchRequest request) {
        if (request.timestamp() == null) {
            throw new InvalidPunchException("timestamp is required");
        }

        var companyId = CompanyContext.getCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context is missing"));

        log.info("biometric_punch_received tenant_id={} employee_id={} device_id={} timestamp={}",
                companyId, request.employeeId(), request.deviceId(), request.timestamp());

        com.company.hrsystem.employee.entity.Employee employee;
        try {
            employee = findEmployee(companyId, request.employeeId());
        } catch (EmployeeNotFoundException ex) {
            saveAudit(
                    companyId,
                    "BIOMETRIC_PUNCH_EMPLOYEE_NOT_FOUND",
                    Map.of(
                            "employeeId", request.employeeId(),
                            "deviceId", request.deviceId(),
                            "timestamp", request.timestamp().toString()));
            throw ex;
        }

        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new InvalidPunchException("Inactive employee cannot punch attendance");
        }

        var generatedDeviceLogId = generatePunchDeviceLogId(request);
        if (biometricEventLogRepository.existsByDeviceLogIdAndCompanyId(generatedDeviceLogId, companyId)) {
            log.warn("biometric_punch_duplicate_device_log tenant_id={} employee_id={} device_id={} device_log_id={}",
                    companyId, request.employeeId(), request.deviceId(), generatedDeviceLogId);
            saveAudit(
                    companyId,
                    "BIOMETRIC_PUNCH_DUPLICATE",
                    Map.of(
                            "employeeId", request.employeeId(),
                            "deviceId", request.deviceId(),
                            "timestamp", request.timestamp().toString()));
            return new BiometricPunchResponse(
                    request.employeeId(),
                    request.timestamp(),
                    request.deviceId(),
                    "DUPLICATE",
                    "IGNORED",
                    "Duplicate punch ignored");
        }

        var attendanceOutcome = attendanceService.recordBiometricPunch(
                companyId,
                employee,
                request.timestamp().toInstant(ZoneOffset.UTC),
                generatedDeviceLogId);

        var eventLog = new BiometricEventLog();
        eventLog.setCompanyId(companyId);
        eventLog.setDeviceId(request.deviceId());
        eventLog.setDeviceLogId(generatedDeviceLogId);
        eventLog.setEmployeeCode(employee.getEmployeeCode());
        eventLog.setEventType(mapPunchTypeToBiometricEventType(attendanceOutcome.punchType()));
        eventLog.setEventTime(request.timestamp());
        eventLog.setProcessed(Boolean.TRUE);
        biometricEventLogRepository.save(eventLog);

        if ("DUPLICATE".equals(attendanceOutcome.punchType())) {
            log.warn("biometric_punch_duplicate tenant_id={} employee_id={} device_id={} timestamp={}",
                    companyId, request.employeeId(), request.deviceId(), request.timestamp());
        }

        log.info("biometric_punch_processed tenant_id={} employee_id={} punch_type={} status={}",
                companyId, request.employeeId(), attendanceOutcome.punchType(), attendanceOutcome.status());

        saveAudit(
                companyId,
                "BIOMETRIC_PUNCH_" + attendanceOutcome.punchType(),
                Map.of(
                        "employeeId", request.employeeId(),
                        "deviceId", request.deviceId(),
                        "timestamp", request.timestamp().toString(),
                        "status", attendanceOutcome.status(),
                        "message", attendanceOutcome.message()));

        return new BiometricPunchResponse(
                request.employeeId(),
                request.timestamp(),
                request.deviceId(),
                attendanceOutcome.punchType(),
                attendanceOutcome.status(),
                attendanceOutcome.message());
    }

    private com.company.hrsystem.employee.entity.Employee findEmployee(UUID companyId, String employeeId) {
        var byCode = biometricEmployeeLookupRepository.findByCompanyIdAndEmployeeCode(companyId, employeeId);
        if (byCode.isPresent()) {
            return byCode.get();
        }

        try {
            var id = UUID.fromString(employeeId);
            return biometricEmployeeLookupRepository.findByIdAndCompanyId(id, companyId)
                    .orElseThrow(() -> {
                        log.error("biometric_punch_employee_not_found tenant_id={} employee_id={}", companyId, employeeId);
                        return new EmployeeNotFoundException("Employee not found for biometric punch");
                    });
        } catch (IllegalArgumentException invalidUuid) {
            log.error("biometric_punch_employee_not_found tenant_id={} employee_id={}", companyId, employeeId);
            throw new EmployeeNotFoundException("Employee not found for biometric punch");
        }
    }

    private String generatePunchDeviceLogId(BiometricPunchRequest request) {
        return (request.deviceId()
                + "-"
                + request.employeeId()
                + "-"
                + request.timestamp())
                .replace(" ", "")
                .toUpperCase(Locale.ROOT);
    }

    private String mapPunchTypeToBiometricEventType(String punchType) {
        return switch (punchType) {
            case "IN" -> "CHECK_IN";
            case "OUT" -> "CHECK_OUT";
            default -> punchType;
        };
    }

    private void publishAfterCommit(BiometricEventReceived event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    applicationEventPublisher.publishEvent(event);
                }
            });
            return;
        }
        applicationEventPublisher.publishEvent(event);
    }

    private void saveAudit(UUID companyId, String eventType, Map<String, String> payload) {
        var auditEntry = new CommonAuditEntry();
        auditEntry.setCompanyId(companyId);
        auditEntry.setEventType(eventType);
        auditEntry.setPayload(payload.toString());
        commonAuditEntryRepository.save(auditEntry);
    }
}
