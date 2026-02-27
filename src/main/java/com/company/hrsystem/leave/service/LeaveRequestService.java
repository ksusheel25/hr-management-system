package com.company.hrsystem.leave.service;

import com.company.hrsystem.auth.security.CustomUserDetails;
import com.company.hrsystem.common.audit.Auditable;
import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.employee.entity.Employee;
import com.company.hrsystem.employee.repository.EmployeeRepository;
import com.company.hrsystem.leave.dto.LeaveApplyRequest;
import com.company.hrsystem.leave.dto.LeaveRequestCreateRequest;
import com.company.hrsystem.leave.dto.LeaveRequestDto;
import com.company.hrsystem.leave.entity.LeaveBalance;
import com.company.hrsystem.leave.entity.LeaveRequest;
import com.company.hrsystem.leave.entity.LeaveStatus;
import com.company.hrsystem.leave.entity.LeaveType;
import com.company.hrsystem.leave.repository.LeaveBalanceRepository;
import com.company.hrsystem.leave.repository.LeaveRequestRepository;
import com.company.hrsystem.leave.repository.LeaveTypeRepository;
import com.company.hrsystem.notification.entity.NotificationType;
import com.company.hrsystem.notification.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private static final Logger log = LoggerFactory.getLogger(LeaveRequestService.class);
    private static final String WFH_LEAVE_TYPE = "WFH";
    private static final List<LeaveStatus> OVERLAP_BLOCKING_STATUSES = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final NotificationService notificationService;

    @Transactional
    public LeaveRequestDto create(LeaveRequestCreateRequest request) {
        var companyId = requireCompanyId();
        validateDateRange(request.fromDate(), request.toDate());

        var employee = getEmployeeForTenant(companyId, request.employeeId());
        var saved = createLeaveRequest(
                companyId,
                employee.getId(),
                request.leaveType(),
                request.fromDate(),
                request.toDate(),
                request.reason());
        log.info("leave_created tenant_id={} leave_request_id={} employee_id={}",
                companyId, saved.getId(), saved.getEmployeeId());
        return toDto(saved);
    }

    @Transactional
    @Auditable(action = "LEAVE_APPLY", module = "LEAVE")
    public LeaveRequestDto applyForCurrentEmployee(LeaveApplyRequest request) {
        var companyId = requireCompanyId();
        validateDateRange(request.fromDate(), request.toDate());

        var currentEmployeeId = requireCurrentEmployeeId();
        getEmployeeForTenant(companyId, currentEmployeeId);
        var saved = createLeaveRequest(
                companyId,
                currentEmployeeId,
                request.leaveType(),
                request.fromDate(),
                request.toDate(),
                request.reason());
        notifyManagerForNewLeaveRequest(companyId, currentEmployeeId, saved);
        log.info("leave_applied tenant_id={} leave_request_id={} employee_id={}",
                companyId, saved.getId(), saved.getEmployeeId());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> list() {
        var companyId = requireCompanyId();
        return leaveRequestRepository.findByCompanyIdOrderByFromDateDesc(companyId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> listMyRequests() {
        var companyId = requireCompanyId();
        var currentEmployeeId = requireCurrentEmployeeId();
        return leaveRequestRepository.findByCompanyIdAndEmployeeIdOrderByFromDateDesc(companyId, currentEmployeeId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> listPendingForCurrentManager() {
        var companyId = requireCompanyId();
        var managerEmployeeId = requireCurrentEmployeeId();
        return leaveRequestRepository.findPendingByManagerId(companyId, LeaveStatus.PENDING, managerEmployeeId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @Auditable(action = "LEAVE_APPROVE", module = "LEAVE")
    public LeaveRequestDto approve(UUID leaveRequestId) {
        return approve(leaveRequestId, null);
    }

    @Transactional
    @Auditable(action = "LEAVE_APPROVE", module = "LEAVE")
    public LeaveRequestDto approve(UUID leaveRequestId, String remarks) {
        return updateStatusByManager(leaveRequestId, LeaveStatus.APPROVED, remarks);
    }

    @Transactional
    @Auditable(action = "LEAVE_REJECT", module = "LEAVE")
    public LeaveRequestDto reject(UUID leaveRequestId) {
        return reject(leaveRequestId, null);
    }

    @Transactional
    @Auditable(action = "LEAVE_REJECT", module = "LEAVE")
    public LeaveRequestDto reject(UUID leaveRequestId, String remarks) {
        return updateStatusByManager(leaveRequestId, LeaveStatus.REJECTED, remarks);
    }

    @Transactional
    public LeaveRequestDto cancel(UUID leaveRequestId) {
        var companyId = requireCompanyId();
        var leaveRequest = leaveRequestRepository.findByIdAndCompanyId(leaveRequestId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found"));
        var currentEmployeeId = requireCurrentEmployeeId();
        if (!leaveRequest.getEmployeeId().equals(currentEmployeeId)) {
            throw new AccessDeniedException("Only the requesting employee can cancel leave");
        }
        if (leaveRequest.getStatus() != LeaveStatus.PENDING && leaveRequest.getStatus() != LeaveStatus.APPROVED) {
            throw new IllegalStateException("Only pending or approved leave requests can be cancelled");
        }
        return updateStatus(companyId, leaveRequest, LeaveStatus.CANCELLED, null, null);
    }

    private LeaveRequestDto updateStatusByManager(UUID leaveRequestId, LeaveStatus status, String remarks) {
        var companyId = requireCompanyId();
        var leaveRequest = leaveRequestRepository.findByIdAndCompanyId(leaveRequestId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found"));
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only pending leave requests can be approved or rejected");
        }

        var managerEmployeeId = requireCurrentEmployeeId();
        assertManagerCanApprove(companyId, leaveRequest, managerEmployeeId);
        return updateStatus(companyId, leaveRequest, status, managerEmployeeId, remarks);
    }

    private LeaveRequestDto updateStatus(
            UUID companyId,
            LeaveRequest leaveRequest,
            LeaveStatus status,
            UUID approverEmployeeId,
            String remarks) {
        var currentStatus = leaveRequest.getStatus();
        if (currentStatus == status) {
            return toDto(leaveRequest);
        }

        if (status == LeaveStatus.APPROVED && !isWfhLeaveType(leaveRequest.getLeaveType())) {
            var leaveType = resolveLeaveType(companyId, leaveRequest.getLeaveType());
            var requestedDaysByYear = computeRequestedDaysByYear(leaveRequest.getFromDate(), leaveRequest.getToDate());
            deductLeaveBalance(companyId, leaveRequest.getEmployeeId(), leaveType.getId(), requestedDaysByYear);
        } else if (currentStatus == LeaveStatus.APPROVED
                && (status == LeaveStatus.REJECTED || status == LeaveStatus.CANCELLED)
                && !isWfhLeaveType(leaveRequest.getLeaveType())) {
            var leaveType = resolveLeaveType(companyId, leaveRequest.getLeaveType());
            var requestedDaysByYear = computeRequestedDaysByYear(leaveRequest.getFromDate(), leaveRequest.getToDate());
            restoreLeaveBalance(companyId, leaveRequest.getEmployeeId(), leaveType.getId(), requestedDaysByYear);
        }

        leaveRequest.setStatus(status);
        leaveRequest.setApproverId(approverEmployeeId);
        leaveRequest.setRemarks(normalizeNullable(remarks));
        var saved = leaveRequestRepository.save(leaveRequest);
        notifyRequesterOnDecision(companyId, saved, status);
        log.info("leave_status_updated tenant_id={} leave_request_id={} from_status={} to_status={} approver_employee_id={}",
                companyId, leaveRequest.getId(), currentStatus, status, approverEmployeeId);
        return toDto(saved);
    }

    private LeaveRequest createLeaveRequest(
            UUID companyId,
            UUID employeeId,
            String leaveType,
            LocalDate fromDate,
            LocalDate toDate,
            String reason) {
        validateNoOverlappingRequest(companyId, employeeId, fromDate, toDate);
        var leaveRequest = new LeaveRequest();
        leaveRequest.setCompanyId(companyId);
        leaveRequest.setTenantId(companyId);
        leaveRequest.setEmployeeId(employeeId);
        leaveRequest.setFromDate(fromDate);
        leaveRequest.setToDate(toDate);
        leaveRequest.setLeaveType(normalizeLeaveType(leaveType));
        leaveRequest.setReason(normalizeNullable(reason));
        leaveRequest.setStatus(LeaveStatus.PENDING);
        leaveRequest.setApproverId(null);
        leaveRequest.setRemarks(null);
        return leaveRequestRepository.save(leaveRequest);
    }

    private void assertManagerCanApprove(UUID companyId, LeaveRequest leaveRequest, UUID managerEmployeeId) {
        var requestingEmployee = getEmployeeForTenant(companyId, leaveRequest.getEmployeeId());
        if (requestingEmployee.getManagerId() == null || !requestingEmployee.getManagerId().equals(managerEmployeeId)) {
            throw new AccessDeniedException("Only the reporting manager can approve or reject this leave request");
        }
    }

    private void notifyManagerForNewLeaveRequest(UUID companyId, UUID employeeId, LeaveRequest leaveRequest) {
        var employee = getEmployeeForTenant(companyId, employeeId);
        if (employee.getManagerId() == null) {
            throw new IllegalStateException("Reporting manager is not assigned for employee");
        }

        var manager = getEmployeeForTenant(companyId, employee.getManagerId());
        var title = "New Leave Request";
        var message = "Employee "
                + employee.getFirstName()
                + " "
                + employee.getLastName()
                + " applied from "
                + leaveRequest.getFromDate()
                + " to "
                + leaveRequest.getToDate();

        notificationService.createNotification(
                companyId,
                manager.getId(),
                title,
                message,
                NotificationType.LEAVE_REQUEST,
                leaveRequest.getId());
    }

    private void notifyRequesterOnDecision(UUID companyId, LeaveRequest leaveRequest, LeaveStatus status) {
        NotificationType notificationType;
        String title;
        if (status == LeaveStatus.APPROVED) {
            notificationType = NotificationType.LEAVE_APPROVED;
            title = "Leave Request Approved";
        } else if (status == LeaveStatus.REJECTED) {
            notificationType = NotificationType.LEAVE_REJECTED;
            title = "Leave Request Rejected";
        } else {
            return;
        }

        var message = "Your "
                + leaveRequest.getLeaveType()
                + " request from "
                + leaveRequest.getFromDate()
                + " to "
                + leaveRequest.getToDate()
                + " is "
                + status.name();

        notificationService.createNotification(
                companyId,
                leaveRequest.getEmployeeId(),
                title,
                message,
                notificationType,
                leaveRequest.getId());
    }

    private void validateNoOverlappingRequest(UUID companyId, UUID employeeId, LocalDate fromDate, LocalDate toDate) {
        var overlapping = leaveRequestRepository.findOverlappingRangeForEmployeeAndStatuses(
                companyId,
                employeeId,
                OVERLAP_BLOCKING_STATUSES,
                fromDate,
                toDate);
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("Overlapping leave request already exists in requested date range");
        }
    }

    private Employee getEmployeeForTenant(UUID companyId, UUID employeeId) {
        return employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found in tenant context"));
    }

    private UUID requireCompanyId() {
        return CompanyContext.getCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context is missing"));
    }

    private UUID requireCurrentEmployeeId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new AccessDeniedException("Authenticated employee context is missing");
        }
        if (principal.getEmployeeId() == null) {
            throw new AccessDeniedException("Authenticated user is not mapped to employee profile");
        }
        return principal.getEmployeeId();
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("fromDate must be less than or equal to toDate");
        }
    }

    private LeaveType resolveLeaveType(UUID companyId, String leaveTypeName) {
        return leaveTypeRepository.findByCompanyIdAndNameIgnoreCase(companyId, leaveTypeName)
                .orElseThrow(() -> new EntityNotFoundException("Leave type not found in tenant context"));
    }

    private Map<Integer, Integer> computeRequestedDaysByYear(LocalDate fromDate, LocalDate toDate) {
        var daysByYear = new HashMap<Integer, Integer>();
        for (var date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            daysByYear.merge(date.getYear(), 1, Integer::sum);
        }
        return daysByYear;
    }

    private void deductLeaveBalance(
            UUID companyId,
            UUID employeeId,
            UUID leaveTypeId,
            Map<Integer, Integer> requestedDaysByYear) {
        var balances = loadBalancesForUpdate(companyId, employeeId, leaveTypeId, requestedDaysByYear);

        for (var entry : requestedDaysByYear.entrySet()) {
            var balance = balances.get(entry.getKey());
            if (balance.getRemaining() < entry.getValue()) {
                log.error("leave_approve_failed reason=insufficient_balance tenant_id={} employee_id={} leave_type_id={} year={} remaining={} requested={}",
                        companyId, employeeId, leaveTypeId, entry.getKey(), balance.getRemaining(), entry.getValue());
                throw new IllegalStateException("Insufficient leave balance for year " + entry.getKey());
            }
        }

        for (var entry : requestedDaysByYear.entrySet()) {
            var balance = balances.get(entry.getKey());
            balance.setUsed(balance.getUsed() + entry.getValue());
            balance.setRemaining(Math.max(0, balance.getAllocated() - balance.getUsed()));
        }

        leaveBalanceRepository.saveAll(balances.values());
    }

    private void restoreLeaveBalance(
            UUID companyId,
            UUID employeeId,
            UUID leaveTypeId,
            Map<Integer, Integer> requestedDaysByYear) {
        var balances = loadBalancesForUpdate(companyId, employeeId, leaveTypeId, requestedDaysByYear);

        for (var entry : requestedDaysByYear.entrySet()) {
            var balance = balances.get(entry.getKey());
            balance.setUsed(Math.max(0, balance.getUsed() - entry.getValue()));
            balance.setRemaining(Math.max(0, balance.getAllocated() - balance.getUsed()));
        }

        leaveBalanceRepository.saveAll(balances.values());
    }

    private Map<Integer, LeaveBalance> loadBalancesForUpdate(
            UUID companyId,
            UUID employeeId,
            UUID leaveTypeId,
            Map<Integer, Integer> requestedDaysByYear) {
        var balances = new HashMap<Integer, LeaveBalance>(requestedDaysByYear.size());
        var missingYears = new ArrayList<Integer>();

        for (var year : requestedDaysByYear.keySet()) {
            var balance = leaveBalanceRepository.findByCompanyIdAndEmployeeIdAndLeaveTypeIdAndYearForUpdate(
                            companyId,
                            employeeId,
                            leaveTypeId,
                            year)
                    .orElse(null);
            if (balance == null) {
                missingYears.add(year);
                continue;
            }
            balances.put(year, balance);
        }

        if (!missingYears.isEmpty()) {
            log.error("leave_balance_missing tenant_id={} employee_id={} leave_type_id={} years={}",
                    companyId, employeeId, leaveTypeId, missingYears);
            throw new EntityNotFoundException("Leave balance not configured for year(s): " + missingYears);
        }

        return balances;
    }

    private boolean isWfhLeaveType(String leaveType) {
        return leaveType != null && WFH_LEAVE_TYPE.equalsIgnoreCase(leaveType.trim());
    }

    private String normalizeLeaveType(String leaveType) {
        if (leaveType == null || leaveType.isBlank()) {
            throw new IllegalArgumentException("leaveType is required");
        }
        return leaveType.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LeaveRequestDto toDto(LeaveRequest leaveRequest) {
        return new LeaveRequestDto(
                leaveRequest.getId(),
                leaveRequest.getEmployeeId(),
                leaveRequest.getFromDate(),
                leaveRequest.getToDate(),
                leaveRequest.getLeaveType(),
                leaveRequest.getReason(),
                leaveRequest.getStatus(),
                leaveRequest.getApproverId(),
                leaveRequest.getRemarks());
    }
}
