package com.company.hrsystem.employee.service;

import com.company.hrsystem.auth.security.CustomUserDetails;
import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.employee.dto.EmployeeDto;
import com.company.hrsystem.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeSelfService {

    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public EmployeeDto getMyProfile() {
        var companyId = CompanyContext.getCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context is missing"));
        var employeeId = requireCurrentEmployeeId();
        var employee = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found for authenticated user"));
        return new EmployeeDto(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getActive(),
                employee.getManagerId(),
                employee.getRemainingWfhBalance());
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

