package com.company.hrsystem.employee.dto;

import java.util.UUID;

public record EmployeeDto(
        UUID id,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        Boolean active,
        UUID managerId,
        Integer remainingWfhBalance) {
}
