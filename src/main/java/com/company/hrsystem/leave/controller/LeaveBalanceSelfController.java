package com.company.hrsystem.leave.controller;

import com.company.hrsystem.leave.dto.MyLeaveBalanceDto;
import com.company.hrsystem.leave.service.LeaveBalanceSelfService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leave-balances")
@PreAuthorize("hasRole('EMPLOYEE')")
@RequiredArgsConstructor
public class LeaveBalanceSelfController {

    private final LeaveBalanceSelfService leaveBalanceSelfService;

    @GetMapping("/me")
    public List<MyLeaveBalanceDto> myBalances() {
        return leaveBalanceSelfService.myBalances();
    }
}

