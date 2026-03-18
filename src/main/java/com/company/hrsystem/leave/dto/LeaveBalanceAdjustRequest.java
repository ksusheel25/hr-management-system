package com.company.hrsystem.leave.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record LeaveBalanceAdjustRequest(
        @NotNull UUID employeeId,
        @NotNull UUID leaveTypeId,
        @NotNull Integer year,
        @NotNull Integer deltaAllocated,
        @NotNull Integer deltaUsed) {
}

