package com.company.hrsystem.leave.controller;

import com.company.hrsystem.leave.dto.LeaveBalanceCreateRequest;
import com.company.hrsystem.leave.dto.LeaveBalanceDto;
import com.company.hrsystem.leave.dto.LeaveBalanceUpdateRequest;
import com.company.hrsystem.leave.service.LeaveBalanceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/leave-balances")
@PreAuthorize("hasRole('HR')")
@RequiredArgsConstructor
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    @PostMapping
    public LeaveBalanceDto createOrUpdate(@Valid @RequestBody LeaveBalanceCreateRequest request) {
        return leaveBalanceService.createOrUpdate(request);
    }

    @GetMapping
    public List<LeaveBalanceDto> list(@RequestParam(required = false) UUID employeeId) {
        return leaveBalanceService.list(employeeId);
    }

    @PutMapping("/{balanceId}")
    public LeaveBalanceDto update(@PathVariable UUID balanceId, @Valid @RequestBody LeaveBalanceUpdateRequest request) {
        return leaveBalanceService.update(balanceId, request);
    }

    @DeleteMapping("/{balanceId}")
    public ResponseEntity<Void> delete(@PathVariable UUID balanceId) {
        leaveBalanceService.delete(balanceId);
        return ResponseEntity.noContent().build();
    }
}
