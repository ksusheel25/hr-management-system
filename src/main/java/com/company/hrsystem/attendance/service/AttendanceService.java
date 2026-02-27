package com.company.hrsystem.attendance.service;

import com.company.hrsystem.attendance.dto.AttendanceResponse;
import com.company.hrsystem.attendance.entity.AttendanceEvent;
import com.company.hrsystem.attendance.entity.AttendanceEventType;
import com.company.hrsystem.attendance.entity.AttendanceSource;
import com.company.hrsystem.attendance.entity.DailySummary;
import com.company.hrsystem.attendance.repository.AttendanceEventRepository;
import com.company.hrsystem.attendance.repository.DailySummaryRepository;
import com.company.hrsystem.auth.security.CustomUserDetails;
import com.company.hrsystem.common.audit.Auditable;
import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.employee.entity.Employee;
import com.company.hrsystem.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceEventRepository attendanceEventRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    @Auditable(action = "MANUAL_ATTENDANCE_CHECK_IN", module = "ATTENDANCE")
    public AttendanceResponse checkIn(UUID employeeId) {
        var companyId = requireCompanyId();
        requireRequesterMatchesEmployee(employeeId);
        var employee = getEmployeeForCompany(companyId, employeeId);

        if (!Boolean.TRUE.equals(employee.getActive())) {
            log.error("attendance_check_in_failed reason=inactive_employee tenant_id={} employee_id={}", companyId, employeeId);
            throw new IllegalStateException("Employee is inactive and cannot check in");
        }

        if (attendanceEventRepository.findLatestOpenSession(companyId, employeeId).isPresent()) {
            log.error("attendance_check_in_failed reason=open_session_exists tenant_id={} employee_id={}", companyId, employeeId);
            throw new IllegalStateException("Open attendance session already exists for employee");
        }

        var now = Instant.now();
        var checkInEvent = new AttendanceEvent();
        checkInEvent.setCompanyId(companyId);
        checkInEvent.setEmployee(employee);
        checkInEvent.setEventType(AttendanceEventType.CHECK_IN);
        checkInEvent.setSource(AttendanceSource.REMOTE);
        checkInEvent.setEventTime(now);
        checkInEvent.setDeviceLogId(nextDeviceLogId("IN"));

        attendanceEventRepository.save(checkInEvent);
        log.info("attendance_checked_in tenant_id={} employee_id={} event_time={}", companyId, employeeId, now);

        return new AttendanceResponse(
                employeeId,
                "CHECKED_IN",
                toLocalDateTime(now),
                null,
                null);
    }

    @Transactional
    @Auditable(action = "MANUAL_ATTENDANCE_CHECK_OUT", module = "ATTENDANCE")
    public AttendanceResponse checkOut(UUID employeeId) {
        var companyId = requireCompanyId();
        requireRequesterMatchesEmployee(employeeId);
        var employee = getEmployeeForCompany(companyId, employeeId);

        var openSession = attendanceEventRepository.findLatestOpenSession(companyId, employeeId)
                .orElseThrow(() -> new IllegalStateException("No open check-in session found for employee"));

        var checkOutTime = Instant.now();
        var checkOutEvent = new AttendanceEvent();
        checkOutEvent.setCompanyId(companyId);
        checkOutEvent.setEmployee(employee);
        checkOutEvent.setEventType(AttendanceEventType.CHECK_OUT);
        checkOutEvent.setSource(AttendanceSource.REMOTE);
        checkOutEvent.setEventTime(checkOutTime);
        checkOutEvent.setDeviceLogId(nextDeviceLogId("OUT"));

        attendanceEventRepository.save(checkOutEvent);
        log.info("attendance_checked_out tenant_id={} employee_id={} event_time={}", companyId, employeeId, checkOutTime);

        var workedMinutes = Math.max(0L, Duration.between(openSession.getEventTime(), checkOutTime).toMinutes());
        upsertDailySummary(companyId, employee, checkOutTime, workedMinutes);

        return new AttendanceResponse(
                employeeId,
                "CHECKED_OUT",
                toLocalDateTime(openSession.getEventTime()),
                toLocalDateTime(checkOutTime),
                workedMinutes);
    }

    private void upsertDailySummary(UUID companyId, Employee employee, Instant checkOutTime, long workedMinutes) {
        var summaryDate = LocalDateTime.ofInstant(checkOutTime, ZoneOffset.UTC).toLocalDate();
        var summary = dailySummaryRepository.findByCompanyIdAndEmployee_IdAndDate(
                        companyId,
                        employee.getId(),
                        summaryDate)
                .orElseGet(() -> newDailySummary(companyId, employee, summaryDate));

        var existingWorked = summary.getTotalWorkedMinutes() == null ? 0L : summary.getTotalWorkedMinutes();
        summary.setTotalWorkedMinutes(existingWorked + workedMinutes);
        summary.setDate(summaryDate);
        summary.setOvertimeMinutes(summary.getOvertimeMinutes() == null ? 0 : summary.getOvertimeMinutes());
        dailySummaryRepository.save(summary);
    }

    private DailySummary newDailySummary(UUID companyId, Employee employee, LocalDate summaryDate) {
        var summary = new DailySummary();
        summary.setCompanyId(companyId);
        summary.setEmployee(employee);
        summary.setDate(summaryDate);
        summary.setTotalWorkedMinutes(0L);
        summary.setOfficeWorkedMinutes(0L);
        summary.setOfficePresent(Boolean.FALSE);
        summary.setRemoteDay(Boolean.FALSE);
        summary.setLateMinutes(0L);
        summary.setEarlyExitMinutes(0L);
        summary.setLateArrival(Boolean.FALSE);
        summary.setEarlyExit(Boolean.FALSE);
        summary.setFinalized(Boolean.FALSE);
        summary.setOvertimeMinutes(0);
        return summary;
    }

    private Employee getEmployeeForCompany(UUID companyId, UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .filter(employee -> companyId.equals(employee.getCompanyId()))
                .orElseThrow(() -> new EntityNotFoundException("Employee not found for company"));
    }

    private UUID requireCompanyId() {
        return CompanyContext.getCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context is missing"));
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private String nextDeviceLogId(String direction) {
        return "REMOTE-" + direction + "-" + UUID.randomUUID();
    }

    private void requireRequesterMatchesEmployee(UUID employeeId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new AccessDeniedException("Authenticated employee context is missing");
        }
        if (principal.getEmployeeId() == null || !principal.getEmployeeId().equals(employeeId)) {
            throw new AccessDeniedException("Employees can only perform attendance actions for themselves");
        }
    }
}
