package com.company.hrsystem.leave.service;

import com.company.hrsystem.leave.entity.LeaveBalance;
import com.company.hrsystem.leave.entity.LeaveType;
import com.company.hrsystem.leave.repository.LeaveBalanceRepository;
import com.company.hrsystem.leave.repository.LeaveTypeRepository;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveEntitlementService {

    public enum DefaultLeaveType {
        ANNUAL,
        SICK,
        WFH
    }

    private static final Map<DefaultLeaveType, Integer> DEFAULT_QUOTAS = new EnumMap<>(DefaultLeaveType.class);

    static {
        DEFAULT_QUOTAS.put(DefaultLeaveType.SICK, 2);
        DEFAULT_QUOTAS.put(DefaultLeaveType.ANNUAL, 14);
        DEFAULT_QUOTAS.put(DefaultLeaveType.WFH, 24);
    }

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    @Transactional
    public Map<DefaultLeaveType, LeaveType> ensureDefaultLeaveTypes(UUID companyId) {
        var result = new EnumMap<DefaultLeaveType, LeaveType>(DefaultLeaveType.class);
        for (var type : DefaultLeaveType.values()) {
            var leaveType = leaveTypeRepository.findByCompanyIdAndNameIgnoreCase(companyId, type.name())
                    .orElseGet(() -> {
                        var created = new LeaveType();
                        created.setCompanyId(companyId);
                        created.setTenantId(companyId);
                        created.setName(type.name());
                        created.setAnnualQuota(DEFAULT_QUOTAS.get(type));
                        created.setCarryForwardAllowed(Boolean.TRUE);
                        return leaveTypeRepository.save(created);
                    });
            result.put(type, leaveType);
        }
        return Map.copyOf(result);
    }

    @Transactional
    public void ensureDefaultBalancesForEmployee(UUID companyId, UUID employeeId, int year) {
        var defaults = ensureDefaultLeaveTypes(companyId);
        for (var entry : defaults.entrySet()) {
            ensureBalance(companyId, employeeId, entry.getValue(), year, DEFAULT_QUOTAS.get(entry.getKey()));
        }
    }

    /**
     * Ensures balances for the provided years only for the default leave types (ANNUAL/SICK/WFH).
     * For non-default leave types, balances must be configured by HR explicitly.
     */
    @Transactional
    public void ensureBalancesForDefaultLeaveTypeYears(
            UUID companyId,
            UUID employeeId,
            UUID leaveTypeId,
            Set<Integer> years) {
        var leaveType = leaveTypeRepository.findByIdAndCompanyId(leaveTypeId, companyId).orElse(null);
        if (leaveType == null) {
            return;
        }
        var defaultType = parseDefaultType(leaveType.getName());
        if (defaultType.isEmpty()) {
            return;
        }

        var quota = DEFAULT_QUOTAS.get(defaultType.get());
        for (var year : years) {
            ensureBalance(companyId, employeeId, leaveType, year, quota);
        }
    }

    private Optional<DefaultLeaveType> parseDefaultType(String name) {
        if (name == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(DefaultLeaveType.valueOf(name.trim().toUpperCase()));
        } catch (IllegalArgumentException ignore) {
            return Optional.empty();
        }
    }

    private void ensureBalance(UUID companyId, UUID employeeId, LeaveType leaveType, int year, int allocated) {
        leaveBalanceRepository.findByCompanyIdAndEmployeeIdAndLeaveTypeIdAndYear(companyId, employeeId, leaveType.getId(), year)
                .orElseGet(() -> {
                    var balance = new LeaveBalance();
                    balance.setCompanyId(companyId);
                    balance.setTenantId(companyId);
                    balance.setEmployeeId(employeeId);
                    balance.setLeaveTypeId(leaveType.getId());
                    balance.setYear(year);
                    balance.setAllocated(Math.max(0, allocated));
                    balance.setUsed(0);
                    balance.setRemaining(Math.max(0, allocated));
                    return leaveBalanceRepository.save(balance);
                });
    }

    public static int currentYear() {
        return LocalDate.now().getYear();
    }
}

