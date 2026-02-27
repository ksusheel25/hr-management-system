package com.company.hrsystem.attendance.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttendanceResponse(
        UUID employeeId,
        String status,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        Long workedMinutes) {
}
