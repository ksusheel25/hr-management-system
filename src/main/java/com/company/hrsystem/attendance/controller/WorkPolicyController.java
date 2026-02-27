package com.company.hrsystem.attendance.controller;

import com.company.hrsystem.attendance.dto.WorkPolicyDto;
import com.company.hrsystem.attendance.dto.WorkPolicyUpdateRequest;
import com.company.hrsystem.attendance.service.WorkPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/work-policy")
@PreAuthorize("hasRole('HR')")
@RequiredArgsConstructor
public class WorkPolicyController {

    private final WorkPolicyService workPolicyService;

    @GetMapping
    public WorkPolicyDto getPolicy() {
        return workPolicyService.getPolicy();
    }

    @PutMapping
    public WorkPolicyDto updatePolicy(@Valid @RequestBody WorkPolicyUpdateRequest request) {
        return workPolicyService.updatePolicy(request);
    }
}
