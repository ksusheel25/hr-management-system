package com.company.hrsystem.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record EmployeeUpdateRequest(
        @NotBlank @Size(max = 50) String employeeCode,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        UUID shiftId,
        UUID managerId,
        Boolean active,
        @PositiveOrZero Integer remainingWfhBalance) {
}
