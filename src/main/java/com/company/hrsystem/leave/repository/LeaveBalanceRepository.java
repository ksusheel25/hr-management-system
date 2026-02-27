package com.company.hrsystem.leave.repository;

import com.company.hrsystem.leave.entity.LeaveBalance;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

    Optional<LeaveBalance> findByIdAndCompanyId(UUID id, UUID companyId);

    Optional<LeaveBalance> findByCompanyIdAndEmployeeIdAndLeaveTypeIdAndYear(
            UUID companyId,
            UUID employeeId,
            UUID leaveTypeId,
            Integer year);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select lb from LeaveBalance lb
            where lb.companyId = :companyId
              and lb.employeeId = :employeeId
              and lb.leaveTypeId = :leaveTypeId
              and lb.year = :year
            """)
    Optional<LeaveBalance> findByCompanyIdAndEmployeeIdAndLeaveTypeIdAndYearForUpdate(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("leaveTypeId") UUID leaveTypeId,
            @Param("year") Integer year);

    List<LeaveBalance> findByCompanyIdOrderByYearDescEmployeeIdAsc(UUID companyId);

    List<LeaveBalance> findByCompanyIdAndEmployeeIdOrderByYearDesc(UUID companyId, UUID employeeId);
}
