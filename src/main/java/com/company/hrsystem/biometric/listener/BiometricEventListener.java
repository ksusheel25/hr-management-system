package com.company.hrsystem.biometric.listener;

import com.company.hrsystem.attendance.entity.AttendanceEvent;
import com.company.hrsystem.attendance.entity.AttendanceEventType;
import com.company.hrsystem.attendance.entity.AttendanceSource;
import com.company.hrsystem.attendance.entity.OfficePresenceSummary;
import com.company.hrsystem.attendance.repository.AttendanceEventRepository;
import com.company.hrsystem.attendance.repository.OfficePresenceSummaryRepository;
import com.company.hrsystem.biometric.entity.BiometricEventLog;
import com.company.hrsystem.biometric.entity.BiometricEventType;
import com.company.hrsystem.biometric.event.BiometricEventReceived;
import com.company.hrsystem.biometric.repository.BiometricEmployeeLookupRepository;
import com.company.hrsystem.biometric.repository.BiometricEventLogRepository;
import com.company.hrsystem.employee.entity.Employee;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BiometricEventListener {

    private final BiometricEventLogRepository biometricEventLogRepository;
    private final BiometricEmployeeLookupRepository biometricEmployeeLookupRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final OfficePresenceSummaryRepository officePresenceSummaryRepository;

    @Async
    @EventListener
    @Transactional
    public void handle(BiometricEventReceived event) {
        var eventLog = biometricEventLogRepository.findByIdAndCompanyId(event.biometricEventLogId(), event.companyId())
                .orElseThrow(() -> new EntityNotFoundException("Biometric event log not found"));

        if (Boolean.TRUE.equals(eventLog.getProcessed())) {
            return;
        }

        var employee = biometricEmployeeLookupRepository
                .findByCompanyIdAndEmployeeCode(event.companyId(), eventLog.getEmployeeCode())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found for employeeCode"));

        var biometricEventType = BiometricEventType.from(eventLog.getEventType());
        saveAttendanceEvent(eventLog, employee, biometricEventType);
        handleOfficePresence(eventLog, employee, biometricEventType);

        eventLog.setProcessed(Boolean.TRUE);
        biometricEventLogRepository.save(eventLog);
    }

    private void saveAttendanceEvent(BiometricEventLog eventLog, Employee employee, BiometricEventType biometricEventType) {
        var attendanceEvent = new AttendanceEvent();
        attendanceEvent.setCompanyId(eventLog.getCompanyId());
        attendanceEvent.setEmployee(employee);
        attendanceEvent.setEventType(mapEventType(biometricEventType));
        attendanceEvent.setSource(AttendanceSource.BIOMETRIC);
        attendanceEvent.setDeviceLogId(eventLog.getCompanyId() + ":" + eventLog.getDeviceLogId());
        attendanceEvent.setEventTime(eventLog.getEventTime().toInstant(ZoneOffset.UTC));
        attendanceEventRepository.save(attendanceEvent);
    }

    private void handleOfficePresence(BiometricEventLog eventLog, Employee employee, BiometricEventType biometricEventType) {
        switch (biometricEventType) {
            case OFFICE_ENTRY -> {
                var summary = new OfficePresenceSummary();
                summary.setCompanyId(eventLog.getCompanyId());
                summary.setEmployee(employee);
                summary.setBusinessDate(eventLog.getEventTime().toLocalDate());
                summary.setOfficeEntryTime(eventLog.getEventTime());
                summary.setOfficeDurationMinutes(0L);
                officePresenceSummaryRepository.save(summary);
            }
            case OFFICE_EXIT -> {
                var openSummary = officePresenceSummaryRepository
                        .findTopByCompanyIdAndEmployee_IdAndOfficeEntryTimeIsNotNullAndOfficeExitTimeIsNullOrderByOfficeEntryTimeDesc(
                                eventLog.getCompanyId(),
                                employee.getId())
                        .orElseThrow(() -> new IllegalStateException("No open office entry found for office exit event"));

                openSummary.setOfficeExitTime(eventLog.getEventTime());
                var durationMinutes = Math.max(0L,
                        Duration.between(openSummary.getOfficeEntryTime(), eventLog.getEventTime()).toMinutes());
                openSummary.setOfficeDurationMinutes(durationMinutes);
                officePresenceSummaryRepository.save(openSummary);
            }
            case CHECK_IN, CHECK_OUT -> {
                // No office presence computation required for check-in/check-out events.
            }
        }
    }

    private AttendanceEventType mapEventType(BiometricEventType biometricEventType) {
        return switch (biometricEventType) {
            case CHECK_IN -> AttendanceEventType.CHECK_IN;
            case CHECK_OUT -> AttendanceEventType.CHECK_OUT;
            case OFFICE_ENTRY -> AttendanceEventType.OFFICE_ENTRY;
            case OFFICE_EXIT -> AttendanceEventType.OFFICE_EXIT;
        };
    }
}
