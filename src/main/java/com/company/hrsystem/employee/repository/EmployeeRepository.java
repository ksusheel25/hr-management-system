package com.company.hrsystem.employee.repository;

import com.company.hrsystem.employee.entity.Employee;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findByCompanyId(UUID companyId);

    @EntityGraph(attributePaths = {"shift"})
    List<Employee> findWithShiftByCompanyId(UUID companyId);

    Optional<Employee> findByCompanyIdAndEmployeeCode(UUID companyId, String employeeCode);

    List<Employee> findByCompanyIdAndActiveTrue(UUID companyId);

    Optional<Employee> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndEmployeeCodeIgnoreCase(UUID companyId, String employeeCode);

    boolean existsByCompanyIdAndEmailIgnoreCase(UUID companyId, String email);
}
