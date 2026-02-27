package com.company.hrsystem.leave.dto;

import com.company.hrsystem.leave.entity.LeaveStatus;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestDto(
        UUID id,
        UUID employeeId,
        LocalDate fromDate,
        LocalDate toDate,
        String leaveType,
        String reason,
        LeaveStatus status,
        UUID approverId,
        String remarks) {
}
