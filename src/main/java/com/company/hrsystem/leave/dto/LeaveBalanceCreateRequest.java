package com.company.hrsystem.leave.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record LeaveBalanceCreateRequest(
        @NotNull UUID employeeId,
        @NotNull UUID leaveTypeId,
        @NotNull Integer year,
        @NotNull @PositiveOrZero Integer allocated,
        @NotNull @PositiveOrZero Integer used) {
}
