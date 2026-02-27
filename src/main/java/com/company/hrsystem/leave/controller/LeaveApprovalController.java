package com.company.hrsystem.leave.controller;

import com.company.hrsystem.leave.dto.LeaveRequestDto;
import com.company.hrsystem.leave.service.LeaveRequestService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leave/approve")
@RequiredArgsConstructor
public class LeaveApprovalController {

    private final LeaveRequestService leaveRequestService;

    @PatchMapping("/{leaveRequestId}")
    @PreAuthorize("hasRole('HR')")
    public LeaveRequestDto approve(@PathVariable UUID leaveRequestId) {
        return leaveRequestService.approve(leaveRequestId);
    }
}
