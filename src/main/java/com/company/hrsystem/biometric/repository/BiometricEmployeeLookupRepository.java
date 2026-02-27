package com.company.hrsystem.biometric.repository;

import com.company.hrsystem.employee.entity.Employee;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BiometricEmployeeLookupRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByCompanyIdAndEmployeeCode(UUID companyId, String employeeCode);
}
