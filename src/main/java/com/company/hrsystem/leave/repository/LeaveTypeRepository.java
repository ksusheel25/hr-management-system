package com.company.hrsystem.leave.repository;

import com.company.hrsystem.leave.entity.LeaveType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {

    List<LeaveType> findByCompanyIdOrderByNameAsc(UUID companyId);

    Optional<LeaveType> findByIdAndCompanyId(UUID id, UUID companyId);

    Optional<LeaveType> findByCompanyIdAndNameIgnoreCase(UUID companyId, String name);

    boolean existsByCompanyIdAndNameIgnoreCase(UUID companyId, String name);
}
