package com.company.hrsystem.leave.dto;

import java.util.List;

public record BulkUploadResultDto(
        int totalRows,
        int successCount,
        int failedCount,
        List<BulkUploadRowErrorDto> errorDetails) {
}
