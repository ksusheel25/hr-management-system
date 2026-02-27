package com.company.hrsystem.attendance.service;

import com.company.hrsystem.attendance.entity.AttendanceEvent;
import com.company.hrsystem.attendance.entity.AttendanceEventType;
import com.company.hrsystem.attendance.entity.AttendanceMode;
import com.company.hrsystem.attendance.entity.AttendanceSource;
import com.company.hrsystem.attendance.entity.AttendanceStatus;
import com.company.hrsystem.attendance.entity.DailySummary;
import com.company.hrsystem.attendance.repository.AttendanceEventRepository;
import com.company.hrsystem.attendance.repository.DailySummaryRepository;
import com.company.hrsystem.company.repository.CompanyRepository;
import com.company.hrsystem.employee.entity.Employee;
import com.company.hrsystem.employee.repository.EmployeeRepository;
import com.company.hrsystem.holiday.repository.HolidayRepository;
import com.company.hrsystem.leave.entity.LeaveRequest;
import com.company.hrsystem.leave.entity.LeaveStatus;
import com.company.hrsystem.leave.repository.LeaveRequestRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
public class AttendanceFinalizationJob {

    private static final Logger log = LoggerFactory.getLogger(AttendanceFinalizationJob.class);
    private static final long REQUIRED_MINUTES = 480L;
    private static final String WFH_LEAVE_TYPE = "WFH";
    private static final long JOB_LOCK_KEY = 810210011L;
    private static final Duration MAX_LOCK_DURATION = Duration.ofMinutes(30);

    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PostgresDistributedLockManager distributedLockManager;
    private final PlatformTransactionManager transactionManager;

    @Scheduled(cron = "0 10 0 * * ?")
    public void finalizeDailyAttendance() {
        var startedAt = Instant.now();
        log.info("attendance_finalization_job_start lock_key={} max_lock_minutes={}",
                JOB_LOCK_KEY, MAX_LOCK_DURATION.toMinutes());

        var optionalLock = distributedLockManager.tryAcquire(JOB_LOCK_KEY);
        if (optionalLock.isEmpty()) {
            log.info("attendance_finalization_job_skip reason=lock_not_acquired lock_key={}", JOB_LOCK_KEY);
            return;
        }

        var deadline = startedAt.plus(MAX_LOCK_DURATION);
        var companies = companyRepository.findAll();
        var txTemplate = new TransactionTemplate(transactionManager);
        var processedEmployees = 0;

        try (var lock = optionalLock.get()) {
            for (var company : companies) {
                if (Instant.now().isAfter(deadline)) {
                    log.warn("attendance_finalization_job_timeout processed_employees={} max_lock_minutes={}",
                            processedEmployees, MAX_LOCK_DURATION.toMinutes());
                    break;
                }

                var companyId = company.getCompanyId() != null ? company.getCompanyId() : company.getId();
                try {
                    var finalizedInCompany = txTemplate.execute(status ->
                            finalizeCompanyDate(companyId, company.getTimezone()));
                    processedEmployees += finalizedInCompany == null ? 0 : finalizedInCompany;
                } catch (RuntimeException ex) {
                    log.error("attendance_finalization_company_failed company_id={}", companyId, ex);
                }
            }
        }

        var durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        log.info("attendance_finalization_job_end processed_employees={} duration_ms={}",
                processedEmployees, durationMs);
    }

    private int finalizeCompanyDate(UUID companyId, String timezone) {
        var zoneId = resolveZone(timezone);
        var targetDate = LocalDate.now(zoneId).minusDays(1);
        var dayStart = targetDate.atStartOfDay(zoneId).toInstant();
        var nextDayStart = targetDate.plusDays(1).atStartOfDay(zoneId).toInstant();

        var activeEmployees = employeeRepository.findByCompanyIdAndActiveTrue(companyId);
        if (activeEmployees.isEmpty()) {
            return 0;
        }

        var summaries = dailySummaryRepository.findByCompanyIdAndDate(companyId, targetDate);
        var summaryByEmployeeId = new HashMap<UUID, DailySummary>(summaries.size());
        for (var summary : summaries) {
            summaryByEmployeeId.put(summary.getEmployee().getId(), summary);
        }
        var holidayExists = holidayRepository.existsByCompanyIdAndDate(companyId, targetDate);
        var approvedLeaves = leaveRequestRepository.findActiveForDate(companyId, LeaveStatus.APPROVED, targetDate);
        var leaveByEmployeeId = mapApprovedLeavesByEmployee(approvedLeaves);

        var events = attendanceEventRepository.findByCompanyAndEventTypesAndEventTimeBetween(
                companyId,
                Set.of(
                        AttendanceEventType.CHECK_IN,
                        AttendanceEventType.CHECK_OUT,
                        AttendanceEventType.OFFICE_ENTRY,
                        AttendanceEventType.OFFICE_EXIT),
                dayStart,
                nextDayStart);
        var eventsByEmployee = groupByEmployee(events);

        var upserts = new ArrayList<DailySummary>(activeEmployees.size());
        for (var employee : activeEmployees) {
            var existingSummary = summaryByEmployeeId.get(employee.getId());
            if (existingSummary != null && Boolean.TRUE.equals(existingSummary.getFinalized())) {
                continue;
            }

            var employeeEvents = eventsByEmployee.getOrDefault(employee.getId(), List.of());
            var totalWorkedMinutes = calculateWorkedMinutes(employeeEvents);
            var officePresent = employeeEvents.stream().anyMatch(event ->
                    event.getSource() == AttendanceSource.BIOMETRIC
                            && (event.getEventType() == AttendanceEventType.CHECK_IN
                            || event.getEventType() == AttendanceEventType.OFFICE_ENTRY));
            var approvedLeave = leaveByEmployeeId.get(employee.getId());
            var onApprovedLeave = approvedLeave != null;
            var onApprovedWfhLeave = onApprovedLeave && isWfhLeaveType(approvedLeave.getLeaveType());
            var attendanceStatus = resolveAttendanceStatus(
                    holidayExists,
                    targetDate,
                    onApprovedLeave,
                    onApprovedWfhLeave,
                    totalWorkedMinutes);
            var attendanceMode = resolveAttendanceMode(attendanceStatus, officePresent, onApprovedWfhLeave);

            var summary = existingSummary != null ? existingSummary : newSummary(companyId, employee, targetDate);
            summary.setTotalWorkedMinutes(totalWorkedMinutes);
            summary.setAttendanceStatus(attendanceStatus);
            summary.setMode(attendanceMode);
            summary.setOfficePresent(attendanceMode == AttendanceMode.OFFICE);
            summary.setRemoteDay(attendanceMode == AttendanceMode.WFH);
            summary.setOfficeWorkedMinutes(attendanceMode == AttendanceMode.OFFICE ? totalWorkedMinutes : 0L);
            summary.setFinalized(Boolean.TRUE);
            upserts.add(summary);
        }

        if (!upserts.isEmpty()) {
            dailySummaryRepository.saveAll(upserts);
        }
        return upserts.size();
    }

    private Map<UUID, List<AttendanceEvent>> groupByEmployee(List<AttendanceEvent> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }
        var grouped = new HashMap<UUID, List<AttendanceEvent>>();
        for (var event : events) {
            grouped.computeIfAbsent(event.getEmployee().getId(), ignored -> new ArrayList<>()).add(event);
        }
        return grouped;
    }

    private Map<UUID, LeaveRequest> mapApprovedLeavesByEmployee(List<LeaveRequest> approvedLeaves) {
        if (approvedLeaves.isEmpty()) {
            return Collections.emptyMap();
        }
        var byEmployee = new HashMap<UUID, LeaveRequest>(approvedLeaves.size());
        for (var leave : approvedLeaves) {
            byEmployee.merge(
                    leave.getEmployeeId(),
                    leave,
                    (current, candidate) -> isWfhLeaveType(candidate.getLeaveType()) ? candidate : current);
        }
        return byEmployee;
    }

    private long calculateWorkedMinutes(List<AttendanceEvent> events) {
        if (events.isEmpty()) {
            return 0L;
        }

        long totalMinutes = 0L;
        Instant openCheckIn = null;

        for (var event : events) {
            if (event.getEventType() == AttendanceEventType.CHECK_IN) {
                if (openCheckIn == null || event.getEventTime().isAfter(openCheckIn)) {
                    openCheckIn = event.getEventTime();
                }
                continue;
            }

            if (event.getEventType() == AttendanceEventType.CHECK_OUT && openCheckIn != null
                    && event.getEventTime().isAfter(openCheckIn)) {
                totalMinutes += Duration.between(openCheckIn, event.getEventTime()).toMinutes();
                openCheckIn = null;
            }
        }

        return Math.max(0L, totalMinutes);
    }

    private AttendanceStatus resolveAttendanceStatus(
            boolean holidayExists,
            LocalDate targetDate,
            boolean onApprovedLeave,
            boolean onApprovedWfhLeave,
            long totalWorkedMinutes) {
        if (holidayExists) {
            return AttendanceStatus.HOLIDAY;
        }
        if (isWeekend(targetDate)) {
            return AttendanceStatus.WEEK_OFF;
        }
        if (onApprovedWfhLeave) {
            return AttendanceStatus.PRESENT;
        }
        if (onApprovedLeave) {
            return AttendanceStatus.ON_LEAVE;
        }
        return totalWorkedMinutes < REQUIRED_MINUTES ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT;
    }

    private boolean isWeekend(LocalDate date) {
        var dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private AttendanceMode resolveAttendanceMode(
            AttendanceStatus attendanceStatus,
            boolean officePresent,
            boolean onApprovedWfhLeave) {
        if (onApprovedWfhLeave) {
            return AttendanceMode.WFH;
        }
        if (attendanceStatus != AttendanceStatus.PRESENT) {
            return null;
        }
        return officePresent ? AttendanceMode.OFFICE : AttendanceMode.WFH;
    }

    private boolean isWfhLeaveType(String leaveType) {
        return leaveType != null && WFH_LEAVE_TYPE.equalsIgnoreCase(leaveType.trim());
    }

    private DailySummary newSummary(UUID companyId, Employee employee, LocalDate date) {
        var summary = new DailySummary();
        summary.setCompanyId(companyId);
        summary.setEmployee(employee);
        summary.setDate(date);
        summary.setTotalWorkedMinutes(0L);
        summary.setOfficeWorkedMinutes(0L);
        summary.setOfficePresent(Boolean.FALSE);
        summary.setRemoteDay(Boolean.FALSE);
        summary.setLateMinutes(0L);
        summary.setEarlyExitMinutes(0L);
        summary.setLateArrival(Boolean.FALSE);
        summary.setEarlyExit(Boolean.FALSE);
        summary.setOvertimeMinutes(0);
        summary.setAttendanceStatus(AttendanceStatus.ABSENT);
        summary.setMode(null);
        summary.setFinalized(Boolean.FALSE);
        return summary;
    }

    private ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception ex) {
            return ZoneOffset.UTC;
        }
    }
}
