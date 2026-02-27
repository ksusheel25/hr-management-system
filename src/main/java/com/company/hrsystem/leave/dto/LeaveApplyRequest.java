package com.company.hrsystem.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record LeaveApplyRequest(
        @NotBlank @Size(max = 80) String leaveType,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @Size(max = 1000) String reason) {
}
