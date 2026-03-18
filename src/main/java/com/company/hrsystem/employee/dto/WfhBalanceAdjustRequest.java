package com.company.hrsystem.employee.dto;

import jakarta.validation.constraints.NotNull;

public record WfhBalanceAdjustRequest(
        @NotNull Integer delta) {
}

