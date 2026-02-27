package com.company.hrsystem.attendance.repository;

import com.company.hrsystem.attendance.entity.OfficePresenceSummary;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfficePresenceSummaryRepository extends JpaRepository<OfficePresenceSummary, UUID> {

    Optional<OfficePresenceSummary> findTopByCompanyIdAndEmployee_IdAndOfficeEntryTimeIsNotNullAndOfficeExitTimeIsNullOrderByOfficeEntryTimeDesc(
            UUID companyId,
            UUID employeeId);

    @Query("""
            select ops.employee.id, coalesce(sum(ops.officeDurationMinutes), 0)
            from OfficePresenceSummary ops
            where ops.companyId = :companyId
              and ops.businessDate = :businessDate
            group by ops.employee.id
            """)
    List<Object[]> findOfficeWorkedMinutesByCompanyAndBusinessDate(
            @Param("companyId") UUID companyId,
            @Param("businessDate") LocalDate businessDate);
}
