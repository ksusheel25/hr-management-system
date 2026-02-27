package com.company.hrsystem.shift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record ShiftCreateRequest(
        @NotBlank @Size(max = 100) String shiftName,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @PositiveOrZero Integer graceMinutes,
        @PositiveOrZero Integer minimumHalfDayMinutes,
        @PositiveOrZero Integer minimumFullDayMinutes) {
}
