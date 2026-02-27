package com.company.hrsystem.shift.dto;

import java.time.LocalTime;
import java.util.UUID;

public record ShiftDto(
        UUID id,
        String shiftName,
        LocalTime startTime,
        LocalTime endTime,
        Integer graceMinutes,
        Integer minimumHalfDayMinutes,
        Integer minimumFullDayMinutes) {
}
