package com.company.hrsystem.leave.controller;

import com.company.hrsystem.leave.dto.LeaveRequestCreateRequest;
import com.company.hrsystem.leave.dto.LeaveRequestDto;
import com.company.hrsystem.leave.service.LeaveRequestService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping
    @PreAuthorize("hasRole('HR')")
    public LeaveRequestDto create(@Valid @RequestBody LeaveRequestCreateRequest request) {
        return leaveRequestService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('HR')")
    public List<LeaveRequestDto> list() {
        return leaveRequestService.list();
    }

    @PatchMapping("/{leaveRequestId}/approve")
    @PreAuthorize("hasRole('HR')")
    public LeaveRequestDto approve(@PathVariable UUID leaveRequestId) {
        return leaveRequestService.approve(leaveRequestId);
    }

    @PatchMapping("/{leaveRequestId}/reject")
    @PreAuthorize("hasRole('HR')")
    public LeaveRequestDto reject(@PathVariable UUID leaveRequestId) {
        return leaveRequestService.reject(leaveRequestId);
    }

    @PatchMapping("/{leaveRequestId}/cancel")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public LeaveRequestDto cancel(@PathVariable UUID leaveRequestId) {
        return leaveRequestService.cancel(leaveRequestId);
    }
}
