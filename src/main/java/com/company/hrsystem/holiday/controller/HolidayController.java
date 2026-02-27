package com.company.hrsystem.holiday.controller;

import com.company.hrsystem.holiday.dto.HolidayCreateRequest;
import com.company.hrsystem.holiday.dto.HolidayDto;
import com.company.hrsystem.holiday.dto.HolidayUpdateRequest;
import com.company.hrsystem.holiday.service.HolidayService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/holidays")
@PreAuthorize("hasRole('HR')")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @PostMapping
    public HolidayDto create(@Valid @RequestBody HolidayCreateRequest request) {
        return holidayService.create(request);
    }

    @GetMapping
    public List<HolidayDto> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return holidayService.list(from, to);
    }

    @PutMapping("/{holidayId}")
    public HolidayDto update(@PathVariable UUID holidayId, @Valid @RequestBody HolidayUpdateRequest request) {
        return holidayService.update(holidayId, request);
    }

    @DeleteMapping("/{holidayId}")
    public ResponseEntity<Void> delete(@PathVariable UUID holidayId) {
        holidayService.delete(holidayId);
        return ResponseEntity.noContent().build();
    }
}
