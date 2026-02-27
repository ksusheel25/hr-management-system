package com.company.hrsystem.attendance.dto;

import java.time.LocalDate;
import java.util.List;

public record MyAttendanceRangeResponse(
        String employeeId,
        LocalDate from,
        LocalDate to,
        int totalDays,
        int presentDays,
        int absentDays,
        int totalPresent,
        int totalAbsent,
        int totalWFH,
        int totalOfficeDays,
        long totalWorkedMinutes,
        List<DailyAttendanceResponse> attendance) {
}
