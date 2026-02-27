package com.company.hrsystem.leave.dto;

import jakarta.validation.constraints.Size;

public record LeaveDecisionRequest(
        @Size(max = 1000) String remarks) {
}
