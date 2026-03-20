package com.company.hrsystem.auth.dto.v2;

import jakarta.validation.constraints.NotBlank;

public record V2RefreshRequest(@NotBlank String refreshToken) {
}

