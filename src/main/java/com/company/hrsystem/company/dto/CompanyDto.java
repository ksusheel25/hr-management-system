package com.company.hrsystem.company.dto;

import java.util.UUID;

public record CompanyDto(UUID id, String name, String code, String timezone) {
}
