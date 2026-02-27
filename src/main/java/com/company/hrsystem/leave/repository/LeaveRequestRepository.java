package com.company.hrsystem.leave.repository;

import com.company.hrsystem.leave.entity.LeaveRequest;
import com.company.hrsystem.leave.entity.LeaveStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    Optional<LeaveRequest> findByIdAndCompanyId(UUID id, UUID companyId);

    List<LeaveRequest> findByCompanyIdOrderByFromDateDesc(UUID companyId);

    List<LeaveRequest> findByCompanyIdAndEmployeeIdOrderByFromDateDesc(UUID companyId, UUID employeeId);

    @Query("""
            select lr from LeaveRequest lr, Employee e
            where lr.companyId = :companyId
              and lr.status = :status
              and e.id = lr.employeeId
              and e.companyId = :companyId
              and e.managerId = :managerId
            order by lr.fromDate desc
            """)
    List<LeaveRequest> findPendingByManagerId(
            @Param("companyId") UUID companyId,
            @Param("status") LeaveStatus status,
            @Param("managerId") UUID managerId);

    @Query("""
            select lr from LeaveRequest lr
            where lr.companyId = :companyId
              and lr.status = :status
              and lr.fromDate <= :date
              and lr.toDate >= :date
            """)
    List<LeaveRequest> findActiveForDate(
            @Param("companyId") UUID companyId,
            @Param("status") LeaveStatus status,
            @Param("date") LocalDate date);

    @Query("""
            select lr from LeaveRequest lr
            where lr.companyId = :companyId
              and lr.employeeId = :employeeId
              and lr.status = :status
              and lr.fromDate <= :toDate
              and lr.toDate >= :fromDate
            """)
    List<LeaveRequest> findOverlappingRangeForEmployee(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("status") LeaveStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            select lr from LeaveRequest lr
            where lr.companyId = :companyId
              and lr.employeeId = :employeeId
              and lr.status in :statuses
              and lr.fromDate <= :toDate
              and lr.toDate >= :fromDate
            """)
    List<LeaveRequest> findOverlappingRangeForEmployeeAndStatuses(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("statuses") Collection<LeaveStatus> statuses,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
