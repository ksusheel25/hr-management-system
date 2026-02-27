package com.company.hrsystem.leave.dto;

import java.util.UUID;

public record LeaveBalanceDto(
        UUID id,
        UUID employeeId,
        UUID leaveTypeId,
        Integer year,
        Integer allocated,
        Integer used,
        Integer remaining) {
}
