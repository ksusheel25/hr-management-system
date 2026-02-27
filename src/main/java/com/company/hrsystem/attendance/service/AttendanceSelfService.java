package com.company.hrsystem.attendance.service;

import com.company.hrsystem.attendance.dto.DailyAttendanceResponse;
import com.company.hrsystem.attendance.dto.MyAttendanceRangeResponse;
import com.company.hrsystem.attendance.entity.AttendanceEventType;
import com.company.hrsystem.attendance.entity.AttendanceMode;
import com.company.hrsystem.attendance.entity.AttendanceSource;
import com.company.hrsystem.attendance.entity.AttendanceStatus;
import com.company.hrsystem.attendance.entity.DailySummary;
import com.company.hrsystem.attendance.repository.AttendanceEventRepository;
import com.company.hrsystem.attendance.repository.DailySummaryRepository;
import com.company.hrsystem.auth.security.CustomUserDetails;
import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.employee.repository.EmployeeRepository;
import com.company.hrsystem.holiday.repository.HolidayRepository;
import com.company.hrsystem.leave.entity.LeaveStatus;
import com.company.hrsystem.leave.repository.LeaveRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceSelfService {

    private static final long REQUIRED_MINUTES_PER_DAY = 480L;
    private static final int MAX_RANGE_DAYS = 90;
    private static final String WFH_LEAVE_TYPE = "WFH";

    private final DailySummaryRepository dailySummaryRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final EmployeeRepository employeeRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Transactional(readOnly = true)
    public MyAttendanceRangeResponse getAttendance(LocalDate from, LocalDate to) {
        validateRange(from, to);

        var companyId = CompanyContext.getCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context is missing"));
        var currentEmployee = employeeRepository.findByIdAndCompanyId(requireCurrentEmployeeId(), companyId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found for authenticated user"));

        var summaries = dailySummaryRepository.findByCompanyIdAndEmployee_IdAndDateBetweenOrderByDateAsc(
                companyId,
                currentEmployee.getId(),
                from,
                to);
        var summaryByDate = new HashMap<LocalDate, DailySummary>(summaries.size());
        for (var summary : summaries) {
            summaryByDate.put(summary.getDate(), summary);
        }
        var holidayDates = holidayRepository.findByCompanyIdAndDateBetweenOrderByDateAsc(companyId, from, to).stream()
                .map(holiday -> holiday.getDate())
                .collect(java.util.stream.Collectors.toSet());
        var approvedLeaves = leaveRequestRepository.findOverlappingRangeForEmployee(
                companyId,
                currentEmployee.getId(),
                LeaveStatus.APPROVED,
                from,
                to);
        var leaveDates = new java.util.HashSet<LocalDate>();
        var wfhLeaveDates = new java.util.HashSet<LocalDate>();
        for (var leave : approvedLeaves) {
            var start = leave.getFromDate().isBefore(from) ? from : leave.getFromDate();
            var end = leave.getToDate().isAfter(to) ? to : leave.getToDate();
            for (var date = start; !date.isAfter(end); date = date.plusDays(1)) {
                if (isWfhLeaveType(leave.getLeaveType())) {
                    wfhLeaveDates.add(date);
                } else {
                    leaveDates.add(date);
                }
            }
        }

        var biometricEvents = attendanceEventRepository.findByCompanyAndEmployeeAndSourceAndEventTypesAndEventTimeBetween(
                companyId,
                currentEmployee.getId(),
                AttendanceSource.BIOMETRIC,
                Set.of(AttendanceEventType.CHECK_IN, AttendanceEventType.OFFICE_ENTRY),
                from.atStartOfDay().toInstant(ZoneOffset.UTC),
                to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        var officeDates = biometricEvents.stream()
                .map(event -> event.getEventTime().atOffset(ZoneOffset.UTC).toLocalDate())
                .collect(java.util.stream.Collectors.toSet());

        var dailyResponses = new ArrayList<DailyAttendanceResponse>();
        long totalWorkedMinutes = 0L;
        int totalPresent = 0;
        int totalAbsent = 0;
        int totalWfh = 0;
        int totalOfficeDays = 0;
        int workingDays = 0;

        for (var date = from; !date.isAfter(to); date = date.plusDays(1)) {
            var summary = summaryByDate.get(date);
            var workedMinutes = summary == null || summary.getTotalWorkedMinutes() == null ? 0L : summary.getTotalWorkedMinutes();
            var shortfallMinutes = Math.max(0L, REQUIRED_MINUTES_PER_DAY - workedMinutes);
            var workedHours = workedMinutes / 60.0;
            var status = resolveStatus(summary, date, workedMinutes, holidayDates, leaveDates, wfhLeaveDates);
            var mode = resolveMode(summary, date, officeDates, status, workedMinutes);
            var lateArrival = summary != null && Boolean.TRUE.equals(summary.getLateArrival());

            if (AttendanceStatus.PRESENT.name().equals(status)) {
                totalPresent++;
                if (AttendanceMode.OFFICE.name().equals(mode)) {
                    totalOfficeDays++;
                } else if (AttendanceMode.WFH.name().equals(mode)) {
                    totalWfh++;
                }
            }
            if (AttendanceStatus.ABSENT.name().equals(status)) {
                totalAbsent++;
            }
            if (!AttendanceStatus.HOLIDAY.name().equals(status) && !AttendanceStatus.WEEK_OFF.name().equals(status)) {
                workingDays++;
            }
            totalWorkedMinutes += workedMinutes;

            dailyResponses.add(new DailyAttendanceResponse(
                    date,
                    status,
                    mode,
                    workedMinutes,
                    workedHours,
                    REQUIRED_MINUTES_PER_DAY,
                    shortfallMinutes,
                    lateArrival));
        }

        return new MyAttendanceRangeResponse(
                currentEmployee.getEmployeeCode(),
                from,
                to,
                workingDays,
                totalPresent,
                totalAbsent,
                totalPresent,
                totalAbsent,
                totalWfh,
                totalOfficeDays,
                totalWorkedMinutes,
                List.copyOf(dailyResponses));
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to dates are required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from date must be less than or equal to to date");
        }
        var rangeDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (rangeDays > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("Date range cannot exceed " + MAX_RANGE_DAYS + " days");
        }
    }

    private java.util.UUID requireCurrentEmployeeId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authenticated user not found");
        }
        if (!(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new AccessDeniedException("Authenticated employee identity is not available");
        }
        if (principal.getEmployeeId() == null) {
            throw new AccessDeniedException("Authenticated user is not mapped to employee profile");
        }
        return principal.getEmployeeId();
    }

    private String resolveStatus(
            DailySummary summary,
            LocalDate date,
            long workedMinutes,
            java.util.Set<LocalDate> holidayDates,
            java.util.Set<LocalDate> leaveDates,
            java.util.Set<LocalDate> wfhLeaveDates) {
        if (summary != null && summary.getAttendanceStatus() != null) {
            return summary.getAttendanceStatus().name();
        }
        if (holidayDates.contains(date)) {
            return AttendanceStatus.HOLIDAY.name();
        }
        if (isWeekend(date)) {
            return AttendanceStatus.WEEK_OFF.name();
        }
        if (leaveDates.contains(date)) {
            return AttendanceStatus.ON_LEAVE.name();
        }
        if (wfhLeaveDates.contains(date)) {
            return AttendanceStatus.PRESENT.name();
        }
        return workedMinutes < REQUIRED_MINUTES_PER_DAY ? AttendanceStatus.ABSENT.name() : AttendanceStatus.PRESENT.name();
    }

    private String resolveMode(
            DailySummary summary,
            LocalDate date,
            java.util.Set<LocalDate> officeDates,
            String status,
            long workedMinutes) {
        if (AttendanceStatus.HOLIDAY.name().equals(status)
                || AttendanceStatus.WEEK_OFF.name().equals(status)
                || AttendanceStatus.ON_LEAVE.name().equals(status)) {
            return null;
        }
        if (summary != null && summary.getMode() != null) {
            return summary.getMode().name();
        }
        if (AttendanceStatus.PRESENT.name().equals(status) && workedMinutes == 0L) {
            return AttendanceMode.WFH.name();
        }
        if (officeDates.contains(date)) {
            return AttendanceMode.OFFICE.name();
        }
        if (workedMinutes > 0) {
            return AttendanceMode.WFH.name();
        }
        return null;
    }

    private boolean isWeekend(LocalDate date) {
        var day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private boolean isWfhLeaveType(String leaveType) {
        return leaveType != null && WFH_LEAVE_TYPE.equalsIgnoreCase(leaveType.trim());
    }
}
