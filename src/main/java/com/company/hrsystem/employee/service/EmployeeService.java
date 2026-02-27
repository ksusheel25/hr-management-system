package com.company.hrsystem.employee.service;

import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.company.entity.Company;
import com.company.hrsystem.company.repository.CompanyRepository;
import com.company.hrsystem.employee.dto.EmployeeCreateRequest;
import com.company.hrsystem.employee.dto.EmployeeDto;
import com.company.hrsystem.employee.dto.EmployeeUpdateRequest;
import com.company.hrsystem.employee.entity.Employee;
import com.company.hrsystem.employee.repository.EmployeeRepository;
import com.company.hrsystem.shift.entity.Shift;
import com.company.hrsystem.shift.repository.ShiftRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final ShiftRepository shiftRepository;

    @Transactional
    public EmployeeDto createEmployee(EmployeeCreateRequest request) {
        var companyId = requireCompanyId();
        var employee = new Employee();
        employee.setCompanyId(companyId);
        employee.setCompany(findCompanyByCompanyId(companyId));
        employee.setShift(resolveShift(companyId, request.shiftId()));
        employee.setManagerId(resolveManagerId(companyId, request.managerId(), null));
        employee.setEmployeeCode(request.employeeCode());
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setActive(Boolean.TRUE);
        employee.setRemainingWfhBalance(defaultZero(request.remainingWfhBalance()));
        return toDto(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> listEmployees() {
        var companyId = requireCompanyId();
        return employeeRepository.findWithShiftByCompanyId(companyId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public EmployeeDto updateEmployee(UUID employeeId, EmployeeUpdateRequest request) {
        var companyId = requireCompanyId();
        var employee = getEmployeeForCompany(companyId, employeeId);
        employee.setShift(resolveShift(companyId, request.shiftId()));
        employee.setManagerId(resolveManagerId(companyId, request.managerId(), employeeId));
        employee.setEmployeeCode(request.employeeCode());
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        if (request.active() != null) {
            employee.setActive(request.active());
        }
        if (request.remainingWfhBalance() != null) {
            employee.setRemainingWfhBalance(defaultZero(request.remainingWfhBalance()));
        }
        return toDto(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeDto deactivateEmployee(UUID employeeId) {
        var companyId = requireCompanyId();
        var employee = getEmployeeForCompany(companyId, employeeId);
        employee.setActive(Boolean.FALSE);
        return toDto(employeeRepository.save(employee));
    }

    private UUID requireCompanyId() {
        return CompanyContext.getCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context is missing"));
    }

    private Company findCompanyByCompanyId(UUID companyId) {
        return companyRepository.findAll().stream()
                .filter(company -> companyId.equals(company.getCompanyId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Company not found in tenant context"));
    }

    private Employee getEmployeeForCompany(UUID companyId, UUID employeeId) {
        return employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found in tenant context"));
    }

    private Shift resolveShift(UUID companyId, UUID shiftId) {
        if (shiftId == null) {
            return null;
        }
        return shiftRepository.findById(shiftId)
                .filter(shift -> companyId.equals(shift.getCompanyId()))
                .orElseThrow(() -> new EntityNotFoundException("Shift not found in tenant context"));
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private UUID resolveManagerId(UUID companyId, UUID managerId, UUID selfEmployeeId) {
        if (managerId == null) {
            return null;
        }
        if (selfEmployeeId != null && selfEmployeeId.equals(managerId)) {
            throw new IllegalArgumentException("Employee cannot be assigned as their own manager");
        }

        return employeeRepository.findByIdAndCompanyId(managerId, companyId)
                .map(Employee::getId)
                .orElseThrow(() -> new EntityNotFoundException("Manager not found in tenant context"));
    }

    private EmployeeDto toDto(Employee employee) {
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
}
