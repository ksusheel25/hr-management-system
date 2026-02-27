package com.company.hrsystem.attendance.repository;

import com.company.hrsystem.attendance.entity.WorkPolicy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkPolicyRepository extends JpaRepository<WorkPolicy, UUID> {

    Optional<WorkPolicy> findByCompanyId(UUID companyId);
}
