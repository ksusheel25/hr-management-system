package com.company.hrsystem.auth.dto.v2;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record V2LoginRequest(
        @NotNull UUID tenantId,
        @NotBlank String username,
        @NotBlank String password) {
}

