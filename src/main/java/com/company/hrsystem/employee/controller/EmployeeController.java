package com.company.hrsystem.employee.controller;

import com.company.hrsystem.employee.dto.EmployeeCreateRequest;
import com.company.hrsystem.employee.dto.EmployeeDto;
import com.company.hrsystem.employee.dto.EmployeeUpdateRequest;
import com.company.hrsystem.employee.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/employees")
@PreAuthorize("hasRole('HR')")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public EmployeeDto createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        return employeeService.createEmployee(request);
    }

    @GetMapping
    public List<EmployeeDto> listEmployees() {
        return employeeService.listEmployees();
    }

    @PutMapping("/{employeeId}")
    public EmployeeDto updateEmployee(@PathVariable UUID employeeId, @Valid @RequestBody EmployeeUpdateRequest request) {
        return employeeService.updateEmployee(employeeId, request);
    }

    @PatchMapping("/{employeeId}/deactivate")
    public EmployeeDto deactivateEmployee(@PathVariable UUID employeeId) {
        return employeeService.deactivateEmployee(employeeId);
    }
}
