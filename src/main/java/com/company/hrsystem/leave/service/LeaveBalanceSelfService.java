package com.company.hrsystem.leave.service;

import com.company.hrsystem.auth.security.CustomUserDetails;
import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.leave.dto.MyLeaveBalanceDto;
import com.company.hrsystem.leave.repository.LeaveBalanceRepository;
import com.company.hrsystem.leave.repository.LeaveTypeRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveBalanceSelfService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    @Transactional(readOnly = true)
    public List<MyLeaveBalanceDto> myBalances() {
        var companyId = CompanyContext.getCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context is missing"));
        var employeeId = requireCurrentEmployeeId();
        var balances = leaveBalanceRepository.findByCompanyIdAndEmployeeIdOrderByYearDesc(companyId, employeeId);
        var leaveTypeIds = balances.stream().map(b -> b.getLeaveTypeId()).distinct().toList();
        var leaveTypesById = leaveTypeRepository.findByCompanyIdAndIdIn(companyId, leaveTypeIds).stream()
                .collect(java.util.stream.Collectors.toMap(t -> t.getId(), t -> t.getName()));

        return balances.stream()
                .map(balance -> new MyLeaveBalanceDto(
                        balance.getId(),
                        balance.getEmployeeId(),
                        balance.getLeaveTypeId(),
                        leaveTypesById.getOrDefault(balance.getLeaveTypeId(), null),
                        balance.getYear(),
                        balance.getAllocated(),
                        balance.getUsed(),
                        balance.getRemaining()))
                .toList();
    }

    private UUID requireCurrentEmployeeId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authenticated user not found");
        }
        if (!(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new AccessDeniedException("Authenticated employee identity is not available");
        }
        if (principal.getEmployeeId() == null) {
            throw new AccessDeniedException("Authenticated user is not mapped to employee profile");
        }
        return principal.getEmployeeId();
    }
}

