package com.company.hrsystem.attendance.service;

import com.company.hrsystem.attendance.entity.AttendanceEvent;
import com.company.hrsystem.attendance.entity.AttendanceSource;
import com.company.hrsystem.attendance.entity.AttendanceStatus;
import com.company.hrsystem.attendance.entity.AttendanceEventType;
import com.company.hrsystem.attendance.entity.DailySummary;
import com.company.hrsystem.attendance.entity.WorkPolicy;
import com.company.hrsystem.attendance.repository.AttendanceEventRepository;
import com.company.hrsystem.attendance.repository.DailySummaryRepository;
import com.company.hrsystem.attendance.repository.OfficePresenceSummaryRepository;
import com.company.hrsystem.attendance.repository.WorkPolicyRepository;
import com.company.hrsystem.company.entity.Company;
import com.company.hrsystem.company.repository.CompanyRepository;
import com.company.hrsystem.employee.entity.Employee;
import com.company.hrsystem.employee.repository.EmployeeRepository;
import com.company.hrsystem.shift.entity.Shift;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
public class AttendanceReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AttendanceReconciliationScheduler.class);

    private final CompanyRepository companyRepository;
    private final WorkPolicyRepository workPolicyRepository;
    private final EmployeeRepository employeeRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final OfficePresenceSummaryRepository officePresenceSummaryRepository;
    private final PlatformTransactionManager transactionManager;

    @Scheduled(cron = "0 5 0 * * ?")
    public void reconcileYesterday() {
        var companies = companyRepository.findAll();
        var txTemplate = new TransactionTemplate(transactionManager);

        for (var company : companies) {
            try {
                txTemplate.executeWithoutResult(status -> reconcileCompany(company));
            } catch (RuntimeException ex) {
                var tenantCompanyId = company.getCompanyId() != null ? company.getCompanyId() : company.getId();
                log.error("Attendance reconciliation failed for companyId={}", tenantCompanyId, ex);
            }
        }
    }

    private void reconcileCompany(Company company) {
        var companyId = company.getCompanyId() != null ? company.getCompanyId() : company.getId();
        var zoneId = resolveCompanyZone(company.getTimezone());
        var targetDate = LocalDate.now(zoneId).minusDays(1);
        var dayStart = targetDate.atStartOfDay(zoneId).toInstant();
        var nextDayStart = targetDate.plusDays(1).atStartOfDay(zoneId).toInstant();

        var policy = loadOrCreatePolicy(companyId);
        var employees = employeeRepository.findWithShiftByCompanyId(companyId);
        if (employees.isEmpty()) {
            return;
        }

        var summaries = dailySummaryRepository.findByCompanyIdAndDate(companyId, targetDate);
        var summaryByEmployeeId = new HashMap<UUID, DailySummary>(summaries.size());
        for (var summary : summaries) {
            summaryByEmployeeId.put(summary.getEmployee().getId(), summary);
        }

        var officeEntryEmployeeIds = attendanceEventRepository
                .findDistinctEmployeeIdsByCompanyAndSourceAndEventTypeAndEventTimeBetween(
                        companyId,
                        AttendanceSource.BIOMETRIC,
                        AttendanceEventType.OFFICE_ENTRY,
                        dayStart,
                        nextDayStart);

        var maxShiftDurationMinutes = employees.stream()
                .map(Employee::getShift)
                .filter(this::isShiftConfigured)
                .mapToLong(this::resolveShiftDurationMinutes)
                .max()
                .orElse(0L);

        var punchRangeStart = dayStart.minus(Duration.ofMinutes(maxShiftDurationMinutes));
        var punchRangeEnd = nextDayStart.plus(Duration.ofMinutes(maxShiftDurationMinutes));
        var punchEvents = attendanceEventRepository.findByCompanyAndEventTypesAndEventTimeBetween(
                companyId,
                Set.of(AttendanceEventType.CHECK_IN, AttendanceEventType.CHECK_OUT),
                punchRangeStart,
                punchRangeEnd);
        var punchEventsByEmployeeId = groupPunchEventsByEmployee(punchEvents);

        var officeWorkedMinutesByEmployeeId = fetchOfficeWorkedMinutes(companyId, targetDate);
        var autoDeduct = Boolean.TRUE.equals(policy.getAutoDeduct());

        var toSaveSummaries = new ArrayList<DailySummary>(employees.size());
        var employeesToUpdate = new ArrayList<Employee>();

        for (var employee : employees) {
            var summary = summaryByEmployeeId.computeIfAbsent(
                    employee.getId(),
                    employeeId -> newDailySummary(companyId, employee, targetDate));

            var totalWorkedMinutes = summary.getTotalWorkedMinutes() == null ? 0L : summary.getTotalWorkedMinutes();
            var officeWorkedMinutes = officeWorkedMinutesByEmployeeId.getOrDefault(employee.getId(), 0L);

            var officePresent = officeEntryEmployeeIds.contains(employee.getId());
            var remoteDay = !officePresent && totalWorkedMinutes > 0;
            var minimumWorkingMinutes = resolveMinimumFullDayMinutes(employee.getShift(), policy);
            var halfDayThresholdMinutes = resolveMinimumHalfDayMinutes(employee.getShift(), policy);
            var halfDayAllowed = Boolean.TRUE.equals(policy.getHalfDayAllowed());
            var attendanceStatus = calculateAttendanceStatus(
                    totalWorkedMinutes,
                    minimumWorkingMinutes,
                    halfDayAllowed,
                    halfDayThresholdMinutes);
            var shiftEvaluation = evaluateShiftTiming(
                    employee.getShift(),
                    targetDate,
                    zoneId,
                    punchEventsByEmployeeId.getOrDefault(employee.getId(), List.of()));

            summary.setDate(targetDate);
            summary.setOfficeWorkedMinutes(officeWorkedMinutes);
            summary.setOfficePresent(officePresent);
            summary.setRemoteDay(remoteDay);
            summary.setLateMinutes(shiftEvaluation.lateMinutes());
            summary.setEarlyExitMinutes(shiftEvaluation.earlyExitMinutes());
            summary.setLateArrival(shiftEvaluation.lateArrival());
            summary.setEarlyExit(shiftEvaluation.earlyExit());
            summary.setAttendanceStatus(attendanceStatus);
            toSaveSummaries.add(summary);

            var didDeduct = deductWfhIfEligible(employee, attendanceStatus, remoteDay, autoDeduct);
            if (didDeduct) {
                employeesToUpdate.add(employee);
            }
        }

        dailySummaryRepository.saveAll(toSaveSummaries);
        if (!employeesToUpdate.isEmpty()) {
            employeeRepository.saveAll(employeesToUpdate);
        }
    }

    private WorkPolicy loadOrCreatePolicy(UUID companyId) {
        return workPolicyRepository.findByCompanyId(companyId)
                .orElseGet(() -> workPolicyRepository.save(defaultPolicy(companyId)));
    }

    private WorkPolicy defaultPolicy(UUID companyId) {
        var policy = new WorkPolicy();
        policy.setCompanyId(companyId);
        policy.setAllowedWfhPerMonth(0);
        policy.setAutoDeduct(Boolean.FALSE);
        policy.setMinimumWorkingMinutes(WorkPolicy.DEFAULT_MINIMUM_WORKING_MINUTES);
        policy.setHalfDayAllowed(Boolean.FALSE);
        policy.setHalfDayThresholdMinutes(WorkPolicy.DEFAULT_MINIMUM_WORKING_MINUTES);
        return policy;
    }

    private DailySummary newDailySummary(UUID companyId, Employee employee, LocalDate date) {
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
        summary.setFinalized(Boolean.FALSE);
        summary.setOvertimeMinutes(0);
        summary.setAttendanceStatus(AttendanceStatus.ABSENT);
        return summary;
    }

    private int normalizeMinimumWorkingMinutes(WorkPolicy policy) {
        var configured = policy.getMinimumWorkingMinutes();
        if (configured == null || configured <= 0) {
            return WorkPolicy.DEFAULT_MINIMUM_WORKING_MINUTES;
        }
        return configured;
    }

    private int normalizeHalfDayThreshold(WorkPolicy policy) {
        var configured = policy.getHalfDayThresholdMinutes();
        if (configured == null || configured < 0) {
            return 0;
        }
        return configured;
    }

    private int resolveMinimumFullDayMinutes(Shift shift, WorkPolicy policy) {
        if (shift != null && shift.getMinimumFullDayMinutes() != null && shift.getMinimumFullDayMinutes() > 0) {
            return shift.getMinimumFullDayMinutes();
        }
        return normalizeMinimumWorkingMinutes(policy);
    }

    private int resolveMinimumHalfDayMinutes(Shift shift, WorkPolicy policy) {
        if (shift != null && shift.getMinimumHalfDayMinutes() != null && shift.getMinimumHalfDayMinutes() >= 0) {
            return shift.getMinimumHalfDayMinutes();
        }
        return normalizeHalfDayThreshold(policy);
    }

    private AttendanceStatus calculateAttendanceStatus(
            long totalWorkedMinutes,
            int minimumWorkingMinutes,
            boolean halfDayAllowed,
            int halfDayThresholdMinutes) {
        if (totalWorkedMinutes == 0) {
            return AttendanceStatus.ABSENT;
        }
        if (totalWorkedMinutes < minimumWorkingMinutes) {
            if (halfDayAllowed && totalWorkedMinutes >= halfDayThresholdMinutes) {
                return AttendanceStatus.HALF_DAY;
            }
            return AttendanceStatus.ABSENT;
        }
        return AttendanceStatus.PRESENT;
    }

    private boolean deductWfhIfEligible(
            Employee employee,
            AttendanceStatus status,
            boolean remoteDay,
            boolean autoDeduct) {
        if (status != AttendanceStatus.PRESENT || !remoteDay || !autoDeduct) {
            return false;
        }

        var remaining = employee.getRemainingWfhBalance() == null ? 0 : employee.getRemainingWfhBalance();
        if (remaining <= 0) {
            return false;
        }

        employee.setRemainingWfhBalance(remaining - 1);
        return true;
    }

    private Map<UUID, Long> fetchOfficeWorkedMinutes(UUID companyId, LocalDate date) {
        var results = officePresenceSummaryRepository.findOfficeWorkedMinutesByCompanyAndBusinessDate(companyId, date);
        var values = new HashMap<UUID, Long>(results.size());
        for (var row : results) {
            var employeeId = (UUID) row[0];
            var minutes = ((Number) row[1]).longValue();
            values.put(employeeId, minutes);
        }
        return values;
    }

    private Map<UUID, List<AttendanceEvent>> groupPunchEventsByEmployee(List<AttendanceEvent> punchEvents) {
        if (punchEvents.isEmpty()) {
            return Collections.emptyMap();
        }
        var grouped = new HashMap<UUID, List<AttendanceEvent>>();
        for (var event : punchEvents) {
            grouped.computeIfAbsent(event.getEmployee().getId(), ignored -> new ArrayList<>()).add(event);
        }
        return grouped;
    }

    private ShiftTimingEvaluation evaluateShiftTiming(
            Shift shift,
            LocalDate targetDate,
            ZoneId zoneId,
            List<AttendanceEvent> employeePunchEvents) {
        if (!isShiftConfigured(shift) || employeePunchEvents.isEmpty()) {
            return ShiftTimingEvaluation.none();
        }

        var shiftWindow = buildShiftWindow(shift, targetDate, zoneId);
        var punchWindow = findPunchWindow(employeePunchEvents, shiftWindow);

        long lateMinutes = 0L;
        boolean lateArrival = false;
        if (punchWindow.firstCheckIn() != null) {
            var graceMinutes = normalizeGraceMinutes(shift);
            var effectiveStart = shiftWindow.start().plus(Duration.ofMinutes(graceMinutes));
            if (punchWindow.firstCheckIn().isAfter(effectiveStart)) {
                lateMinutes = Duration.between(effectiveStart, punchWindow.firstCheckIn()).toMinutes();
                lateArrival = lateMinutes > 0;
            }
        }

        long earlyExitMinutes = 0L;
        boolean earlyExit = false;
        if (punchWindow.lastCheckOut() != null && punchWindow.lastCheckOut().isBefore(shiftWindow.end())) {
            earlyExitMinutes = Duration.between(punchWindow.lastCheckOut(), shiftWindow.end()).toMinutes();
            earlyExit = earlyExitMinutes > 0;
        }

        return new ShiftTimingEvaluation(lateMinutes, earlyExitMinutes, lateArrival, earlyExit);
    }

    private ShiftWindow buildShiftWindow(Shift shift, LocalDate targetDate, ZoneId zoneId) {
        var shiftStart = LocalDateTime.of(targetDate, shift.getStartTime());
        var shiftEnd = LocalDateTime.of(targetDate, shift.getEndTime());
        if (!shiftEnd.isAfter(shiftStart)) {
            shiftEnd = shiftEnd.plusDays(1);
        }
        var start = shiftStart.atZone(zoneId).toInstant();
        var end = shiftEnd.atZone(zoneId).toInstant();
        var durationMinutes = Math.max(0L, Duration.between(start, end).toMinutes());
        return new ShiftWindow(start, end, durationMinutes);
    }

    private PunchWindow findPunchWindow(List<AttendanceEvent> events, ShiftWindow shiftWindow) {
        var punchInWindowStart = shiftWindow.start().minus(Duration.ofMinutes(shiftWindow.durationMinutes()));
        var punchInWindowEnd = shiftWindow.end();
        var punchOutWindowStart = shiftWindow.start();
        var punchOutWindowEnd = shiftWindow.end().plus(Duration.ofMinutes(shiftWindow.durationMinutes()));

        Instant firstCheckIn = null;
        Instant lastCheckOut = null;

        for (var event : events) {
            var eventTime = event.getEventTime();
            if (event.getEventType() == AttendanceEventType.CHECK_IN
                    && isWithin(eventTime, punchInWindowStart, punchInWindowEnd)
                    && (firstCheckIn == null || eventTime.isBefore(firstCheckIn))) {
                firstCheckIn = eventTime;
            }

            if (event.getEventType() == AttendanceEventType.CHECK_OUT
                    && isWithin(eventTime, punchOutWindowStart, punchOutWindowEnd)
                    && (lastCheckOut == null || eventTime.isAfter(lastCheckOut))) {
                lastCheckOut = eventTime;
            }
        }

        return new PunchWindow(firstCheckIn, lastCheckOut);
    }

    private long resolveShiftDurationMinutes(Shift shift) {
        if (!isShiftConfigured(shift)) {
            return 0L;
        }
        var window = buildShiftWindow(shift, LocalDate.of(2000, 1, 1), ZoneOffset.UTC);
        return window.durationMinutes();
    }

    private boolean isShiftConfigured(Shift shift) {
        return shift != null && shift.getStartTime() != null && shift.getEndTime() != null;
    }

    private int normalizeGraceMinutes(Shift shift) {
        var grace = shift.getGraceMinutes();
        if (grace == null || grace < 0) {
            return 0;
        }
        return grace;
    }

    private boolean isWithin(Instant value, Instant startInclusive, Instant endInclusive) {
        return !value.isBefore(startInclusive) && !value.isAfter(endInclusive);
    }

    private ZoneId resolveCompanyZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception ex) {
            return ZoneOffset.UTC;
        }
    }

    private record ShiftWindow(Instant start, Instant end, long durationMinutes) {
    }

    private record PunchWindow(Instant firstCheckIn, Instant lastCheckOut) {
    }

    private record ShiftTimingEvaluation(long lateMinutes, long earlyExitMinutes, boolean lateArrival, boolean earlyExit) {
        private static ShiftTimingEvaluation none() {
            return new ShiftTimingEvaluation(0L, 0L, false, false);
        }
    }
}
