package com.company.hrsystem.attendance.controller;

import com.company.hrsystem.attendance.dto.AttendanceResponse;
import com.company.hrsystem.attendance.dto.CheckInRequest;
import com.company.hrsystem.attendance.dto.CheckOutRequest;
import com.company.hrsystem.attendance.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attendance")
@PreAuthorize("hasRole('EMPLOYEE')")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    public AttendanceResponse checkIn(@Valid @RequestBody CheckInRequest request) {
        return attendanceService.checkIn(request.employeeId());
    }

    @PostMapping("/check-out")
    public AttendanceResponse checkOut(@Valid @RequestBody CheckOutRequest request) {
        return attendanceService.checkOut(request.employeeId());
    }
}
