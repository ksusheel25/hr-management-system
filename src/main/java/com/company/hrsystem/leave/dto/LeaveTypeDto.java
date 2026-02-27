package com.company.hrsystem.leave.dto;

import java.util.UUID;

public record LeaveTypeDto(
        UUID id,
        String name,
        Integer annualQuota,
        Boolean carryForwardAllowed) {
}
