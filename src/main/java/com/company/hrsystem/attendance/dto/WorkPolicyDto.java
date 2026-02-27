package com.company.hrsystem.attendance.dto;

import java.util.UUID;

public record WorkPolicyDto(
        UUID id,
        Integer allowedWfhPerMonth,
        Boolean autoDeduct,
        Integer minimumWorkingMinutes,
        Boolean halfDayAllowed,
        Integer halfDayThresholdMinutes) {
}
