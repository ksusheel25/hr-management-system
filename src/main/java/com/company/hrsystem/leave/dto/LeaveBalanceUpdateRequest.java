package com.company.hrsystem.leave.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record LeaveBalanceUpdateRequest(
        @NotNull @PositiveOrZero Integer allocated,
        @NotNull @PositiveOrZero Integer used) {
}
