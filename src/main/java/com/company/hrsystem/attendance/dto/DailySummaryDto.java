package com.company.hrsystem.attendance.dto;

import java.time.LocalDate;
import java.util.UUID;

public record DailySummaryDto(UUID id, UUID employeeId, LocalDate summaryDate, Integer workedMinutes, Integer overtimeMinutes) {
}
