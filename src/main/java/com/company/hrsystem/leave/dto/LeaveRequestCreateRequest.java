package com.company.hrsystem.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestCreateRequest(
        @NotNull UUID employeeId,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @NotBlank @Size(max = 80) String leaveType,
        @Size(max = 1000) String reason) {
}
