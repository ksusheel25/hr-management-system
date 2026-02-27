package com.company.hrsystem.leave.service;

import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.leave.dto.LeaveTypeCreateRequest;
import com.company.hrsystem.leave.dto.LeaveTypeDto;
import com.company.hrsystem.leave.dto.LeaveTypeUpdateRequest;
import com.company.hrsystem.leave.entity.LeaveType;
import com.company.hrsystem.leave.repository.LeaveTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    @Transactional
    public LeaveTypeDto create(LeaveTypeCreateRequest request) {
        var companyId = requireCompanyId();
        if (leaveTypeRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.name())) {
            throw new IllegalStateException("Leave type already exists");
        }

        var leaveType = new LeaveType();
        leaveType.setCompanyId(companyId);
        leaveType.setTenantId(companyId);
        leaveType.setName(request.name().trim());
        leaveType.setAnnualQuota(request.annualQuota());
        leaveType.setCarryForwardAllowed(request.carryForwardAllowed());
        return toDto(leaveTypeRepository.save(leaveType));
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeDto> list() {
        var companyId = requireCompanyId();
        return leaveTypeRepository.findByCompanyIdOrderByNameAsc(companyId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public LeaveTypeDto update(UUID leaveTypeId, LeaveTypeUpdateRequest request) {
        var companyId = requireCompanyId();
        var leaveType = leaveTypeRepository.findByIdAndCompanyId(leaveTypeId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Leave type not found"));

        var normalizedName = request.name().trim();
        if (!leaveType.getName().equalsIgnoreCase(normalizedName)
                && leaveTypeRepository.existsByCompanyIdAndNameIgnoreCase(companyId, normalizedName)) {
            throw new IllegalStateException("Leave type already exists");
        }

        leaveType.setName(normalizedName);
        leaveType.setAnnualQuota(request.annualQuota());
        leaveType.setCarryForwardAllowed(request.carryForwardAllowed());
        return toDto(leaveTypeRepository.save(leaveType));
    }

    @Transactional
    public void delete(UUID leaveTypeId) {
        var companyId = requireCompanyId();
        var leaveType = leaveTypeRepository.findByIdAndCompanyId(leaveTypeId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Leave type not found"));
        leaveTypeRepository.delete(leaveType);
    }

    private UUID requireCompanyId() {
        return CompanyContext.getCompanyId().orElseThrow(() -> new IllegalStateException("Company context is missing"));
    }

    private LeaveTypeDto toDto(LeaveType leaveType) {
        return new LeaveTypeDto(
                leaveType.getId(),
                leaveType.getName(),
                leaveType.getAnnualQuota(),
                leaveType.getCarryForwardAllowed());
    }
}
