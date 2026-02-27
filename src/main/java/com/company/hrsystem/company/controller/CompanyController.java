package com.company.hrsystem.company.controller;

import com.company.hrsystem.company.dto.CompanyCreateRequest;
import com.company.hrsystem.company.dto.CompanyDto;
import com.company.hrsystem.company.dto.CompanyUpdateRequest;
import com.company.hrsystem.company.service.CompanyService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/companies")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public CompanyDto createCompany(@Valid @RequestBody CompanyCreateRequest request) {
        return companyService.createCompany(request);
    }

    @GetMapping("/{companyId}")
    public CompanyDto getCompany(@PathVariable UUID companyId) {
        return companyService.getCompany(companyId);
    }

    @PutMapping("/{companyId}")
    public CompanyDto updateCompany(@PathVariable UUID companyId, @Valid @RequestBody CompanyUpdateRequest request) {
        return companyService.updateCompany(companyId, request);
    }
}
