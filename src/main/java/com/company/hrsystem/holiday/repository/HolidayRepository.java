package com.company.hrsystem.holiday.repository;

import com.company.hrsystem.holiday.entity.Holiday;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, UUID> {

    boolean existsByCompanyIdAndDate(UUID companyId, LocalDate date);

    Optional<Holiday> findByIdAndCompanyId(UUID id, UUID companyId);

    List<Holiday> findByCompanyIdAndDateBetweenOrderByDateAsc(UUID companyId, LocalDate from, LocalDate to);
}
