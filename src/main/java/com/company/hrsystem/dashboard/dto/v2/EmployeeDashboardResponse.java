package com.company.hrsystem.dashboard.dto.v2;

import com.company.hrsystem.attendance.dto.MyAttendanceRangeResponse;
import com.company.hrsystem.employee.dto.EmployeeDto;
import com.company.hrsystem.holiday.dto.HolidayDto;
import com.company.hrsystem.leave.dto.LeaveRequestDto;
import com.company.hrsystem.leave.dto.MyLeaveBalanceDto;
import java.util.List;

public record EmployeeDashboardResponse(
        EmployeeDto profile,
        List<MyLeaveBalanceDto> leaveBalances,
        List<LeaveRequestDto> myLeaves,
        List<HolidayDto> holidays,
        MyAttendanceRangeResponse attendance) {
}

