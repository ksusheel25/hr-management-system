package com.company.hrsystem.attendance.controller;

import com.company.hrsystem.attendance.dto.WorkPolicyDto;
import com.company.hrsystem.attendance.service.WorkPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/work-policy")
@PreAuthorize("hasAnyRole('EMPLOYEE','HR')")
@RequiredArgsConstructor
public class WorkPolicySelfController {

    private final WorkPolicyService workPolicyService;

    @GetMapping
    public WorkPolicyDto getPolicy() {
        return workPolicyService.getPolicy();
    }
}

