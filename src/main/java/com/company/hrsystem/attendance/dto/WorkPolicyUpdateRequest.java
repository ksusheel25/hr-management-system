package com.company.hrsystem.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record WorkPolicyUpdateRequest(
        @NotNull @PositiveOrZero Integer allowedWfhPerMonth,
        @NotNull Boolean autoDeduct,
        @NotNull @PositiveOrZero Integer minimumWorkingMinutes,
        @NotNull Boolean halfDayAllowed,
        @NotNull @PositiveOrZero Integer halfDayThresholdMinutes) {
}
