package com.company.hrsystem.attendance.repository;

import com.company.hrsystem.attendance.entity.DailySummary;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailySummaryRepository extends JpaRepository<DailySummary, UUID> {

    Optional<DailySummary> findByCompanyIdAndEmployee_IdAndDate(
            UUID companyId,
            UUID employeeId,
            LocalDate date);

    List<DailySummary> findByCompanyIdAndDate(UUID companyId, LocalDate date);

    List<DailySummary> findByCompanyIdAndEmployee_IdAndDateBetweenOrderByDateAsc(
            UUID companyId,
            UUID employeeId,
            LocalDate from,
            LocalDate to);
}
