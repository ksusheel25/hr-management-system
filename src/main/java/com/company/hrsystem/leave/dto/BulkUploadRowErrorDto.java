package com.company.hrsystem.leave.dto;

public record BulkUploadRowErrorDto(
        int rowNumber,
        String message) {
}
