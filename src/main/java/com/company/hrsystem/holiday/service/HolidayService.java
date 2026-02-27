package com.company.hrsystem.holiday.service;

import com.company.hrsystem.common.audit.Auditable;
import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.holiday.dto.HolidayCreateRequest;
import com.company.hrsystem.holiday.dto.HolidayDto;
import com.company.hrsystem.holiday.dto.HolidayUpdateRequest;
import com.company.hrsystem.holiday.entity.Holiday;
import com.company.hrsystem.holiday.repository.HolidayRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private static final Logger log = LoggerFactory.getLogger(HolidayService.class);

    private final HolidayRepository holidayRepository;

    @Transactional
    @Auditable(action = "HOLIDAY_CREATE", module = "HOLIDAY")
    public HolidayDto create(HolidayCreateRequest request) {
        var companyId = requireCompanyId();
        if (holidayRepository.existsByCompanyIdAndDate(companyId, request.date())) {
            log.error("holiday_create_failed reason=duplicate_date tenant_id={} date={}", companyId, request.date());
            throw new IllegalStateException("Holiday already exists for date");
        }

        var holiday = new Holiday();
        holiday.setCompanyId(companyId);
        holiday.setTenantId(companyId);
        holiday.setDate(request.date());
        holiday.setName(request.name());
        var saved = holidayRepository.save(holiday);
        log.info("holiday_created tenant_id={} holiday_id={} date={}", companyId, saved.getId(), saved.getDate());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<HolidayDto> list(LocalDate from, LocalDate to) {
        var companyId = requireCompanyId();
        var start = from == null ? LocalDate.now().minusYears(1) : from;
        var end = to == null ? LocalDate.now().plusYears(1) : to;
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("from date must be less than or equal to to date");
        }
        return holidayRepository.findByCompanyIdAndDateBetweenOrderByDateAsc(companyId, start, end)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @Auditable(action = "HOLIDAY_UPDATE", module = "HOLIDAY")
    public HolidayDto update(UUID holidayId, HolidayUpdateRequest request) {
        var companyId = requireCompanyId();
        var holiday = holidayRepository.findByIdAndCompanyId(holidayId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Holiday not found"));

        if (!holiday.getDate().equals(request.date()) && holidayRepository.existsByCompanyIdAndDate(companyId, request.date())) {
            log.error("holiday_update_failed reason=duplicate_date tenant_id={} holiday_id={} date={}",
                    companyId, holidayId, request.date());
            throw new IllegalStateException("Holiday already exists for date");
        }

        holiday.setDate(request.date());
        holiday.setName(request.name());
        var saved = holidayRepository.save(holiday);
        log.info("holiday_updated tenant_id={} holiday_id={} date={}", companyId, holidayId, saved.getDate());
        return toDto(saved);
    }

    @Transactional
    @Auditable(action = "HOLIDAY_DELETE", module = "HOLIDAY")
    public void delete(UUID holidayId) {
        var companyId = requireCompanyId();
        var holiday = holidayRepository.findByIdAndCompanyId(holidayId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Holiday not found"));
        holidayRepository.delete(holiday);
        log.info("holiday_deleted tenant_id={} holiday_id={}", companyId, holidayId);
    }

    private UUID requireCompanyId() {
        return CompanyContext.getCompanyId().orElseThrow(() -> new IllegalStateException("Company context is missing"));
    }

    private HolidayDto toDto(Holiday holiday) {
        return new HolidayDto(holiday.getId(), holiday.getDate(), holiday.getName());
    }
}
