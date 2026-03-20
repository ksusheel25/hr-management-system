package com.company.hrsystem.dashboard.controller;

import com.company.hrsystem.attendance.service.AttendanceSelfService;
import com.company.hrsystem.dashboard.dto.v2.EmployeeDashboardResponse;
import com.company.hrsystem.employee.service.EmployeeSelfService;
import com.company.hrsystem.holiday.service.HolidayService;
import com.company.hrsystem.leave.service.LeaveBalanceSelfService;
import com.company.hrsystem.leave.service.LeaveRequestService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/dashboard")
@RequiredArgsConstructor
public class DashboardV2Controller {

    private final EmployeeSelfService employeeSelfService;
    private final LeaveBalanceSelfService leaveBalanceSelfService;
    private final LeaveRequestService leaveRequestService;
    private final HolidayService holidayService;
    private final AttendanceSelfService attendanceSelfService;

    @GetMapping("/employee")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public EmployeeDashboardResponse employeeDashboard() {
        var profile = employeeSelfService.getMyProfile();
        var balances = leaveBalanceSelfService.myBalances();
        var myLeaves = leaveRequestService.listMyRequests();
        var now = LocalDate.now();
        var holidays = holidayService.list(now.minusMonths(1), now.plusMonths(3));
        var attendance = attendanceSelfService.getAttendance(now.withDayOfMonth(1), now);
        return new EmployeeDashboardResponse(profile, balances, myLeaves, holidays, attendance);
    }
}

