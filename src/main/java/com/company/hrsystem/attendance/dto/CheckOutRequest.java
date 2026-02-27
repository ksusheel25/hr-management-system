package com.company.hrsystem.attendance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CheckOutRequest(@NotNull UUID employeeId) {
}
