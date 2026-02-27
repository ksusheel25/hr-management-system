package com.company.hrsystem.company.service;

import com.company.hrsystem.common.audit.Auditable;
import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.company.dto.CompanyCreateRequest;
import com.company.hrsystem.company.dto.CompanyDto;
import com.company.hrsystem.company.dto.CompanyUpdateRequest;
import com.company.hrsystem.company.entity.Company;
import com.company.hrsystem.company.repository.CompanyRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository companyRepository;

    @Transactional
    @Auditable(action = "TENANT_CREATE", module = "TENANT")
    public CompanyDto createCompany(CompanyCreateRequest request) {
        var company = new Company();
        company.setCompanyId(resolveCompanyIdForCreate());
        company.setName(request.name());
        company.setCode(request.code());
        company.setTimezone(request.timezone());
        var saved = companyRepository.save(company);
        log.info("tenant_created company_id={} id={}", saved.getCompanyId(), saved.getId());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public CompanyDto getCompany(UUID companyId) {
        return toDto(getCompanyForAccess(companyId));
    }

    @Transactional
    public CompanyDto updateCompany(UUID companyId, CompanyUpdateRequest request) {
        var company = getCompanyForAccess(companyId);
        company.setName(request.name());
        company.setCode(request.code());
        company.setTimezone(request.timezone());
        var saved = companyRepository.save(company);
        log.info("tenant_updated company_id={} id={}", saved.getCompanyId(), saved.getId());
        return toDto(saved);
    }

    private Company getCompanyForAccess(UUID companyId) {
        var company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));
        CompanyContext.getCompanyId().ifPresent(contextCompanyId -> {
            if (!contextCompanyId.equals(company.getCompanyId())) {
                throw new EntityNotFoundException("Company not found in tenant context");
            }
        });
        return company;
    }

    private UUID resolveCompanyIdForCreate() {
        return CompanyContext.getCompanyId().orElseGet(UUID::randomUUID);
    }

    private CompanyDto toDto(Company company) {
        return new CompanyDto(company.getId(), company.getName(), company.getCode(), company.getTimezone());
    }
}
