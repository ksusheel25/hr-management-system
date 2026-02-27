package com.company.hrsystem.attendance.dto;

import java.time.LocalDate;

public record DailyAttendanceResponse(
        LocalDate date,
        String status,
        String mode,
        long workedMinutes,
        double workedHours,
        long requiredMinutes,
        long shortfallMinutes,
        boolean lateArrival) {
}
