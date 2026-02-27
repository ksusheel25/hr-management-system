package com.company.hrsystem.config.dto;

import java.util.UUID;

public record ConfigPropertyDto(UUID id, String configKey, String configValue) {
}
