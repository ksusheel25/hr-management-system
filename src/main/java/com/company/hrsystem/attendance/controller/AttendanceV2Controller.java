package com.company.hrsystem.attendance.controller;

import com.company.hrsystem.attendance.dto.AttendanceResponse;
import com.company.hrsystem.attendance.service.AttendanceService;
import com.company.hrsystem.auth.security.CustomUserDetails;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/attendance")
@RequiredArgsConstructor
public class AttendanceV2Controller {

    public record CheckInRequestV2(UUID employeeId) {
    }

    public record CheckOutRequestV2(UUID employeeId) {
    }

    private final AttendanceService attendanceService;

    @PostMapping("/checkin")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public AttendanceResponse checkIn(@RequestBody(required = false) CheckInRequestV2 request) {
        return attendanceService.checkIn(resolveEmployeeId(request == null ? null : request.employeeId()));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public AttendanceResponse checkOut(@RequestBody(required = false) CheckOutRequestV2 request) {
        return attendanceService.checkOut(resolveEmployeeId(request == null ? null : request.employeeId()));
    }

    private UUID resolveEmployeeId(UUID bodyEmployeeId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails principal)
                || principal.getEmployeeId() == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authenticated employee context is missing");
        }
        // If the client passes employeeId, it must match; otherwise we use token identity.
        if (bodyEmployeeId != null && !principal.getEmployeeId().equals(bodyEmployeeId)) {
            throw new org.springframework.security.access.AccessDeniedException("Employees can only perform attendance actions for themselves");
        }
        return principal.getEmployeeId();
    }
}

