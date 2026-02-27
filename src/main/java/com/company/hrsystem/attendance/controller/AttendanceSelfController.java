package com.company.hrsystem.attendance.controller;

import com.company.hrsystem.attendance.dto.MyAttendanceRangeResponse;
import com.company.hrsystem.attendance.service.AttendanceSelfService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attendance")
@PreAuthorize("hasRole('EMPLOYEE')")
@RequiredArgsConstructor
public class AttendanceSelfController {

    private final AttendanceSelfService attendanceSelfService;

    @GetMapping({"/me", "/self"})
    public MyAttendanceRangeResponse getMyAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        var today = LocalDate.now();
        if (from == null && to == null) {
            from = today;
            to = today;
        } else if (from != null && to == null) {
            to = from;
        } else if (from == null) {
            from = to;
        }

        return attendanceSelfService.getAttendance(from, to);
    }
}
