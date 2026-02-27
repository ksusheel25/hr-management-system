package com.company.hrsystem.attendance.service;

import com.company.hrsystem.attendance.dto.WorkPolicyDto;
import com.company.hrsystem.attendance.dto.WorkPolicyUpdateRequest;
import com.company.hrsystem.attendance.entity.WorkPolicy;
import com.company.hrsystem.attendance.repository.WorkPolicyRepository;
import com.company.hrsystem.common.context.CompanyContext;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkPolicyService {

    private final WorkPolicyRepository workPolicyRepository;

    @Transactional(readOnly = true)
    public WorkPolicyDto getPolicy() {
        var companyId = requireCompanyId();
        var policy = workPolicyRepository.findByCompanyId(companyId)
                .orElseGet(() -> workPolicyRepository.save(defaultPolicy(companyId)));
        return toDto(policy);
    }

    @Transactional
    public WorkPolicyDto updatePolicy(WorkPolicyUpdateRequest request) {
        var companyId = requireCompanyId();
        var policy = workPolicyRepository.findByCompanyId(companyId)
                .orElseGet(() -> defaultPolicy(companyId));

        policy.setAllowedWfhPerMonth(nonNegative(request.allowedWfhPerMonth()));
        policy.setAutoDeduct(request.autoDeduct());
        policy.setMinimumWorkingMinutes(nonNegative(request.minimumWorkingMinutes()));
        policy.setHalfDayAllowed(request.halfDayAllowed());
        policy.setHalfDayThresholdMinutes(nonNegative(request.halfDayThresholdMinutes()));

        return toDto(workPolicyRepository.save(policy));
    }

    private UUID requireCompanyId() {
        return CompanyContext.getCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context is missing"));
    }

    private WorkPolicy defaultPolicy(UUID companyId) {
        var policy = new WorkPolicy();
        policy.setCompanyId(companyId);
        policy.setAllowedWfhPerMonth(0);
        policy.setAutoDeduct(Boolean.FALSE);
        policy.setMinimumWorkingMinutes(WorkPolicy.DEFAULT_MINIMUM_WORKING_MINUTES);
        policy.setHalfDayAllowed(Boolean.FALSE);
        policy.setHalfDayThresholdMinutes(0);
        return policy;
    }

    private int nonNegative(Integer value) {
        return Math.max(0, value == null ? 0 : value);
    }

    private WorkPolicyDto toDto(WorkPolicy policy) {
        return new WorkPolicyDto(
                policy.getId(),
                policy.getAllowedWfhPerMonth(),
                policy.getAutoDeduct(),
                policy.getMinimumWorkingMinutes(),
                policy.getHalfDayAllowed(),
                policy.getHalfDayThresholdMinutes());
    }
}
