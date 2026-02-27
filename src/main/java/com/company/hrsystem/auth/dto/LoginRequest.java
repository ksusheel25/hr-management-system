package com.company.hrsystem.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record LoginRequest(
        @NotNull UUID tenantId,
        @NotBlank String username,
        @NotBlank String password) {
}
