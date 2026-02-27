package com.company.hrsystem.shift.service;

import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.company.entity.Company;
import com.company.hrsystem.company.repository.CompanyRepository;
import com.company.hrsystem.shift.dto.ShiftCreateRequest;
import com.company.hrsystem.shift.dto.ShiftDto;
import com.company.hrsystem.shift.dto.ShiftUpdateRequest;
import com.company.hrsystem.shift.entity.Shift;
import com.company.hrsystem.shift.repository.ShiftRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public ShiftDto createShift(ShiftCreateRequest request) {
        var companyId = requireCompanyId();
        var shift = new Shift();
        shift.setCompanyId(companyId);
        shift.setCompany(findCompanyByCompanyId(companyId));
        applyShiftFields(shift, request.shiftName(), request.startTime(), request.endTime(),
                request.graceMinutes(), request.minimumHalfDayMinutes(), request.minimumFullDayMinutes());
        return toDto(shiftRepository.save(shift));
    }

    @Transactional(readOnly = true)
    public List<ShiftDto> listShifts() {
        var companyId = requireCompanyId();
        return shiftRepository.findAll().stream()
                .filter(shift -> companyId.equals(shift.getCompanyId()))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ShiftDto updateShift(UUID shiftId, ShiftUpdateRequest request) {
        var companyId = requireCompanyId();
        var shift = getShiftForCompany(companyId, shiftId);
        applyShiftFields(shift, request.shiftName(), request.startTime(), request.endTime(),
                request.graceMinutes(), request.minimumHalfDayMinutes(), request.minimumFullDayMinutes());
        return toDto(shiftRepository.save(shift));
    }

    @Transactional
    public void deleteShift(UUID shiftId) {
        var companyId = requireCompanyId();
        var shift = getShiftForCompany(companyId, shiftId);
        try {
            shiftRepository.delete(shift);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Shift is assigned to employees and cannot be deleted");
        }
    }

    private UUID requireCompanyId() {
        return CompanyContext.getCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context is missing"));
    }

    private Company findCompanyByCompanyId(UUID companyId) {
        return companyRepository.findAll().stream()
                .filter(company -> companyId.equals(company.getCompanyId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Company not found in tenant context"));
    }

    private Shift getShiftForCompany(UUID companyId, UUID shiftId) {
        return shiftRepository.findById(shiftId)
                .filter(shift -> companyId.equals(shift.getCompanyId()))
                .orElseThrow(() -> new EntityNotFoundException("Shift not found in tenant context"));
    }

    private void applyShiftFields(
            Shift shift,
            String shiftName,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime,
            Integer graceMinutes,
            Integer minimumHalfDayMinutes,
            Integer minimumFullDayMinutes) {
        shift.setShiftName(shiftName);
        shift.setStartTime(startTime);
        shift.setEndTime(endTime);
        shift.setGraceMinutes(normalizeInt(graceMinutes));
        shift.setMinimumHalfDayMinutes(normalizeInt(minimumHalfDayMinutes));
        shift.setMinimumFullDayMinutes(normalizeInt(minimumFullDayMinutes));
    }

    private int normalizeInt(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private ShiftDto toDto(Shift shift) {
        return new ShiftDto(
                shift.getId(),
                shift.getShiftName(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getGraceMinutes(),
                shift.getMinimumHalfDayMinutes(),
                shift.getMinimumFullDayMinutes());
    }
}
