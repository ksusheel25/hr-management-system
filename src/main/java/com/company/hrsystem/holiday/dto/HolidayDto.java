package com.company.hrsystem.holiday.dto;

import java.time.LocalDate;
import java.util.UUID;

public record HolidayDto(UUID id, LocalDate date, String name) {
}
