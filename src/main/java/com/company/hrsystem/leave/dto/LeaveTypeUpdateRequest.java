package com.company.hrsystem.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record LeaveTypeUpdateRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull @PositiveOrZero Integer annualQuota,
        @NotNull Boolean carryForwardAllowed) {
}
