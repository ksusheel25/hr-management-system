package com.company.hrsystem.employee.controller;

import com.company.hrsystem.employee.dto.EmployeeDto;
import com.company.hrsystem.employee.service.EmployeeSelfService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeSelfController {

    private final EmployeeSelfService employeeSelfService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR', 'MANAGER')")
    public EmployeeDto me() {
        return employeeSelfService.getMyProfile();
    }
}

