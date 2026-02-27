package com.company.hrsystem.leave.controller;

import com.company.hrsystem.leave.dto.LeaveApplyRequest;
import com.company.hrsystem.leave.dto.LeaveDecisionRequest;
import com.company.hrsystem.leave.dto.LeaveRequestDto;
import com.company.hrsystem.leave.service.LeaveRequestService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
public class LeaveWorkflowController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping("/apply")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public LeaveRequestDto apply(@Valid @RequestBody LeaveApplyRequest request) {
        return leaveRequestService.applyForCurrentEmployee(request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<LeaveRequestDto> myRequests() {
        return leaveRequestService.listMyRequests();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public List<LeaveRequestDto> pendingRequests() {
        return leaveRequestService.listPendingForCurrentManager();
    }

    @PostMapping("/{leaveRequestId}/approve")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public LeaveRequestDto approve(
            @PathVariable UUID leaveRequestId,
            @Valid @RequestBody(required = false) LeaveDecisionRequest request) {
        var remarks = request == null ? null : request.remarks();
        return leaveRequestService.approve(leaveRequestId, remarks);
    }

    @PostMapping("/{leaveRequestId}/reject")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public LeaveRequestDto reject(
            @PathVariable UUID leaveRequestId,
            @Valid @RequestBody(required = false) LeaveDecisionRequest request) {
        var remarks = request == null ? null : request.remarks();
        return leaveRequestService.reject(leaveRequestId, remarks);
    }
}
