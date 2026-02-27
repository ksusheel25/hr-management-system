package com.company.hrsystem.attendance.dto;

import com.company.hrsystem.attendance.entity.AttendanceEventType;
import com.company.hrsystem.attendance.entity.AttendanceSource;
import java.time.Instant;
import java.util.UUID;

public record AttendanceEventDto(
        UUID id,
        UUID employeeId,
        AttendanceEventType eventType,
        AttendanceSource source,
        String deviceLogId,
        Instant eventTime) {
}
