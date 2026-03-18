package com.company.hrsystem.leave.dto;

import java.util.UUID;

public record MyLeaveBalanceDto(
        UUID id,
        UUID employeeId,
        UUID leaveTypeId,
        String leaveType,
        Integer year,
        Integer allocated,
        Integer used,
        Integer remaining) {
}

