package com.company.hrsystem.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 64) String timezone) {
}
