package com.company.hrsystem.holiday.controller;

import com.company.hrsystem.holiday.dto.HolidayDto;
import com.company.hrsystem.holiday.service.HolidayService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/holidays")
@PreAuthorize("hasAnyRole('EMPLOYEE','HR')")
@RequiredArgsConstructor
public class HolidaySelfController {

    private final HolidayService holidayService;

    @GetMapping
    public List<HolidayDto> list() {
        return holidayService.listAll();
    }
}

