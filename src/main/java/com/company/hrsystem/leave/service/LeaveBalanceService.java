package com.company.hrsystem.leave.service;

import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.employee.repository.EmployeeRepository;
import com.company.hrsystem.leave.dto.LeaveBalanceCreateRequest;
import com.company.hrsystem.leave.dto.LeaveBalanceDto;
import com.company.hrsystem.leave.dto.LeaveBalanceUpdateRequest;
import com.company.hrsystem.leave.entity.LeaveBalance;
import com.company.hrsystem.leave.repository.LeaveBalanceRepository;
import com.company.hrsystem.leave.repository.LeaveTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    @Transactional
    public LeaveBalanceDto createOrUpdate(LeaveBalanceCreateRequest request) {
        var companyId = requireCompanyId();
        validateReferences(companyId, request.employeeId(), request.leaveTypeId());

        var balance = leaveBalanceRepository
                .findByCompanyIdAndEmployeeIdAndLeaveTypeIdAndYear(
                        companyId,
                        request.employeeId(),
                        request.leaveTypeId(),
                        request.year())
                .orElseGet(() -> newBalance(companyId, request.employeeId(), request.leaveTypeId(), request.year()));

        balance.setAllocated(request.allocated());
        balance.setUsed(request.used());
        validateUsage(balance.getAllocated(), balance.getUsed());
        recalculateRemaining(balance);
        return toDto(leaveBalanceRepository.save(balance));
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceDto> list(UUID employeeId) {
        var companyId = requireCompanyId();
        var balances = employeeId == null
                ? leaveBalanceRepository.findByCompanyIdOrderByYearDescEmployeeIdAsc(companyId)
                : leaveBalanceRepository.findByCompanyIdAndEmployeeIdOrderByYearDesc(companyId, employeeId);
        return balances.stream().map(this::toDto).toList();
    }

    @Transactional
    public LeaveBalanceDto update(UUID balanceId, LeaveBalanceUpdateRequest request) {
        var companyId = requireCompanyId();
        var balance = leaveBalanceRepository.findByIdAndCompanyId(balanceId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Leave balance not found"));

        balance.setAllocated(request.allocated());
        balance.setUsed(request.used());
        validateUsage(balance.getAllocated(), balance.getUsed());
        recalculateRemaining(balance);
        return toDto(leaveBalanceRepository.save(balance));
    }

    @Transactional
    public void delete(UUID balanceId) {
        var companyId = requireCompanyId();
        var balance = leaveBalanceRepository.findByIdAndCompanyId(balanceId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Leave balance not found"));
        leaveBalanceRepository.delete(balance);
    }

    private LeaveBalance newBalance(UUID companyId, UUID employeeId, UUID leaveTypeId, Integer year) {
        var balance = new LeaveBalance();
        balance.setCompanyId(companyId);
        balance.setTenantId(companyId);
        balance.setEmployeeId(employeeId);
        balance.setLeaveTypeId(leaveTypeId);
        balance.setYear(year);
        balance.setAllocated(0);
        balance.setUsed(0);
        balance.setRemaining(0);
        return balance;
    }

    private void validateReferences(UUID companyId, UUID employeeId, UUID leaveTypeId) {
        var employeeExists = employeeRepository.findById(employeeId)
                .filter(employee -> companyId.equals(employee.getCompanyId()))
                .isPresent();
        if (!employeeExists) {
            throw new EntityNotFoundException("Employee not found in tenant context");
        }

        leaveTypeRepository.findByIdAndCompanyId(leaveTypeId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Leave type not found in tenant context"));
    }

    private void validateUsage(Integer allocated, Integer used) {
        if (used > allocated) {
            throw new IllegalStateException("Used leave cannot exceed allocated leave");
        }
    }

    private void recalculateRemaining(LeaveBalance balance) {
        balance.setRemaining(Math.max(0, balance.getAllocated() - balance.getUsed()));
    }

    private UUID requireCompanyId() {
        return CompanyContext.getCompanyId().orElseThrow(() -> new IllegalStateException("Company context is missing"));
    }

    private LeaveBalanceDto toDto(LeaveBalance balance) {
        return new LeaveBalanceDto(
                balance.getId(),
                balance.getEmployeeId(),
                balance.getLeaveTypeId(),
                balance.getYear(),
                balance.getAllocated(),
                balance.getUsed(),
                balance.getRemaining());
    }
}
