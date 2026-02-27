package com.company.hrsystem.leave.service;

import com.company.hrsystem.auth.entity.Role;
import com.company.hrsystem.auth.repository.AuthUserRepository;
import com.company.hrsystem.auth.service.AuthUserService;
import com.company.hrsystem.common.audit.Auditable;
import com.company.hrsystem.common.context.CompanyContext;
import com.company.hrsystem.company.entity.Company;
import com.company.hrsystem.company.repository.CompanyRepository;
import com.company.hrsystem.employee.entity.Employee;
import com.company.hrsystem.employee.repository.EmployeeRepository;
import com.company.hrsystem.leave.dto.BulkUploadResultDto;
import com.company.hrsystem.leave.dto.BulkUploadRowErrorDto;
import com.company.hrsystem.leave.entity.LeaveBalance;
import com.company.hrsystem.leave.entity.LeaveType;
import com.company.hrsystem.leave.repository.LeaveBalanceRepository;
import com.company.hrsystem.leave.repository.LeaveTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EmployeeBulkUploadService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeBulkUploadService.class);

    private static final String DEFAULT_PASSWORD = "ChangeMe@123";
    private static final int MAX_ALLOWED_ROWS = 10_000;
    private static final long MAX_ALLOWED_FILE_BYTES = 10L * 1024 * 1024;

    private static final List<String> REQUIRED_HEADERS = List.of(
            "employeeId",
            "firstName",
            "lastName",
            "email",
            "role",
            "managerEmployeeId",
            "joiningDate",
            "leaveBalance");

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DMY_DASH = DateTimeFormatter.ofPattern("dd-MM-uuuu");
    private static final DateTimeFormatter DMY_SLASH = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final AuthUserRepository authUserRepository;
    private final AuthUserService authUserService;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PlatformTransactionManager transactionManager;

    @Auditable(action = "BULK_EMPLOYEE_UPLOAD", module = "EMPLOYEE")
    public BulkUploadResultDto bulkUpload(MultipartFile file) {
        var companyId = requireCompanyId();
        validateFile(file);

        log.info("employee_bulk_upload_start tenant_id={} filename={} size_bytes={}",
                companyId, file.getOriginalFilename(), file.getSize());

        List<RawUploadRow> rows;
        try (var inputStream = file.getInputStream()) {
            rows = parseExcel(inputStream);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to read upload file");
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Upload file has no data rows");
        }
        if (rows.size() > MAX_ALLOWED_ROWS) {
            throw new IllegalArgumentException("Upload file exceeds maximum rows: " + MAX_ALLOWED_ROWS);
        }

        var duplicateEmployeeIds = duplicates(rows, RawUploadRow::employeeId);
        var duplicateEmails = duplicates(rows, RawUploadRow::email);
        var leaveTypes = leaveTypeRepository.findByCompanyIdOrderByNameAsc(companyId);

        var transactionTemplate = new TransactionTemplate(transactionManager);
        var errors = new ArrayList<BulkUploadRowErrorDto>();
        int successCount = 0;

        for (var row : rows) {
            try {
                validateRow(row, companyId, duplicateEmployeeIds, duplicateEmails, leaveTypes);
                transactionTemplate.executeWithoutResult(status ->
                        persistRow(companyId, row, leaveTypes));
                successCount++;
            } catch (Exception ex) {
                errors.add(new BulkUploadRowErrorDto(row.rowNumber(), rootMessage(ex)));
                log.warn("employee_bulk_upload_row_failed tenant_id={} row={} reason={}",
                        companyId, row.rowNumber(), rootMessage(ex));
            }
        }

        var result = new BulkUploadResultDto(
                rows.size(),
                successCount,
                errors.size(),
                List.copyOf(errors));
        log.info("employee_bulk_upload_complete tenant_id={} total_rows={} success_count={} failed_count={}",
                companyId, result.totalRows(), result.successCount(), result.failedCount());
        return result;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is empty");
        }
        if (file.getSize() > MAX_ALLOWED_FILE_BYTES) {
            throw new IllegalArgumentException("Upload file exceeds max size of 10 MB");
        }
    }

    private void validateRow(
            RawUploadRow row,
            UUID companyId,
            Set<String> duplicateEmployeeIds,
            Set<String> duplicateEmails,
            List<LeaveType> leaveTypes) {
        if (leaveTypes.isEmpty()) {
            throw new IllegalStateException("At least one leave type must exist before bulk upload");
        }

        if (row.employeeId().isBlank()) {
            throw new IllegalArgumentException("employeeId is required");
        }
        if (row.firstName().isBlank() || row.lastName().isBlank()) {
            throw new IllegalArgumentException("firstName and lastName are required");
        }
        if (row.email().isBlank() || !row.email().contains("@")) {
            throw new IllegalArgumentException("Valid email is required");
        }
        if (row.role() == null || (row.role() != Role.EMPLOYEE && row.role() != Role.HR)) {
            throw new IllegalArgumentException("role must be EMPLOYEE or HR");
        }
        if (row.joiningDate() == null) {
            throw new IllegalArgumentException("joiningDate is required and must be valid");
        }
        if (row.leaveBalance() < 0) {
            throw new IllegalArgumentException("leaveBalance must be greater than or equal to 0");
        }

        if (duplicateEmployeeIds.contains(row.employeeId().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Duplicate employeeId exists inside file");
        }
        if (duplicateEmails.contains(row.email().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Duplicate email exists inside file");
        }

        if (employeeRepository.existsByCompanyIdAndEmployeeCodeIgnoreCase(companyId, row.employeeId())) {
            throw new IllegalArgumentException("employeeId already exists for tenant");
        }
        if (employeeRepository.existsByCompanyIdAndEmailIgnoreCase(companyId, row.email())) {
            throw new IllegalArgumentException("email already exists for tenant");
        }
        if (authUserRepository.existsByTenantIdAndUsernameIgnoreCase(companyId, row.employeeId())) {
            throw new IllegalArgumentException("Auth username already exists for tenant");
        }

        if (row.managerEmployeeId() != null && !row.managerEmployeeId().isBlank()) {
            var manager = employeeRepository.findByCompanyIdAndEmployeeCode(companyId, row.managerEmployeeId().trim())
                    .orElseThrow(() -> new IllegalArgumentException("managerEmployeeId does not exist in tenant"));
            if (manager.getEmployeeCode().equalsIgnoreCase(row.employeeId())) {
                throw new IllegalArgumentException("Employee cannot be their own manager");
            }
        }
    }

    private void persistRow(UUID companyId, RawUploadRow row, List<LeaveType> leaveTypes) {
        var employee = new Employee();
        employee.setCompanyId(companyId);
        employee.setCompany(findCompanyByCompanyId(companyId));
        employee.setEmployeeCode(row.employeeId());
        employee.setFirstName(row.firstName());
        employee.setLastName(row.lastName());
        employee.setEmail(row.email());
        employee.setActive(Boolean.TRUE);
        employee.setRemainingWfhBalance(0);
        employee.setManagerId(resolveManagerId(companyId, row));
        var savedEmployee = employeeRepository.save(employee);

        authUserService.createUser(
                companyId,
                row.employeeId(),
                DEFAULT_PASSWORD,
                row.role(),
                savedEmployee.getId(),
                true);

        for (var leaveType : leaveTypes) {
            var leaveBalance = new LeaveBalance();
            leaveBalance.setCompanyId(companyId);
            leaveBalance.setTenantId(companyId);
            leaveBalance.setEmployeeId(savedEmployee.getId());
            leaveBalance.setLeaveTypeId(leaveType.getId());
            leaveBalance.setYear(row.joiningDate().getYear());
            leaveBalance.setAllocated(row.leaveBalance());
            leaveBalance.setUsed(0);
            leaveBalance.setRemaining(row.leaveBalance());
            leaveBalanceRepository.save(leaveBalance);
        }
    }

    private UUID resolveManagerId(UUID companyId, RawUploadRow row) {
        if (row.managerEmployeeId() == null || row.managerEmployeeId().isBlank()) {
            return null;
        }
        return employeeRepository.findByCompanyIdAndEmployeeCode(companyId, row.managerEmployeeId().trim())
                .map(Employee::getId)
                .orElseThrow(() -> new IllegalArgumentException("managerEmployeeId does not exist in tenant"));
    }

    private List<RawUploadRow> parseExcel(InputStream inputStream) {
        try (var workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Workbook must contain at least one sheet");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Missing header row");
            }

            var headerIndex = extractHeaderIndex(headerRow);
            for (var required : REQUIRED_HEADERS) {
                if (!headerIndex.containsKey(required.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Missing required column: " + required);
                }
            }

            int lastRowNum = sheet.getLastRowNum();
            var rows = new ArrayList<RawUploadRow>();
            for (int rowNum = 1; rowNum <= lastRowNum; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }

                var raw = mapRow(row, rowNum + 1, headerIndex);
                if (isDataRowEmpty(raw)) {
                    continue;
                }
                rows.add(raw);
            }
            return rows;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Excel file format");
        }
    }

    private Map<String, Integer> extractHeaderIndex(Row headerRow) {
        var map = new HashMap<String, Integer>();
        short lastCellNum = headerRow.getLastCellNum();
        if (lastCellNum < 0) {
            return map;
        }
        for (int i = 0; i < lastCellNum; i++) {
            var header = cellString(headerRow, i);
            if (!header.isBlank()) {
                map.put(header.trim().toLowerCase(Locale.ROOT), i);
            }
        }
        return map;
    }

    private RawUploadRow mapRow(
            Row row,
            int rowNumber,
            Map<String, Integer> headerIndex) {
        return new RawUploadRow(
                rowNumber,
                normalized(cellString(row, headerIndex.get("employeeid"))),
                normalized(cellString(row, headerIndex.get("firstname"))),
                normalized(cellString(row, headerIndex.get("lastname"))),
                normalized(cellString(row, headerIndex.get("email"))),
                parseRole(normalized(cellString(row, headerIndex.get("role")))),
                normalizedNullable(cellString(row, headerIndex.get("manageremployeeid"))),
                parseDate(normalized(cellString(row, headerIndex.get("joiningdate")))),
                parseNonNegativeInt(normalized(cellString(row, headerIndex.get("leavebalance")))));
    }

    private String cellString(Row row, Integer cellIndex) {
        if (cellIndex == null) {
            return "";
        }
        Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case NUMERIC -> numericCellToString(cell);
            case FORMULA -> {
                var cachedType = cell.getCachedFormulaResultType();
                if (cachedType == CellType.NUMERIC) {
                    yield numericCellToString(cell);
                }
                yield String.valueOf(cell);
            }
            case BLANK -> "";
            default -> String.valueOf(cell).trim();
        };
    }

    private String numericCellToString(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString();
        }
        double value = cell.getNumericCellValue();
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private Set<String> duplicates(List<RawUploadRow> rows, java.util.function.Function<RawUploadRow, String> field) {
        var counts = new HashMap<String, Integer>();
        for (var row : rows) {
            var value = normalizedNullable(field.apply(row));
            if (value == null) {
                continue;
            }
            counts.merge(value.toLowerCase(Locale.ROOT), 1, Integer::sum);
        }
        var duplicates = new HashSet<String>();
        counts.forEach((key, count) -> {
            if (count > 1) {
                duplicates.add(key);
            }
        });
        return duplicates;
    }

    private Role parseRole(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (var formatter : List.of(ISO_DATE, DMY_DASH, DMY_SLASH)) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return null;
    }

    private int parseNonNegativeInt(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("leaveBalance is required");
        }
        try {
            var parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new IllegalArgumentException("leaveBalance must be greater than or equal to 0");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("leaveBalance must be a valid integer");
        }
    }

    private boolean isDataRowEmpty(RawUploadRow row) {
        return row.employeeId().isBlank()
                && row.firstName().isBlank()
                && row.lastName().isBlank()
                && row.email().isBlank();
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizedNullable(String value) {
        var normalized = normalized(value);
        return normalized.isBlank() ? null : normalized;
    }

    private Company findCompanyByCompanyId(UUID companyId) {
        return companyRepository.findAll().stream()
                .filter(company -> companyId.equals(company.getCompanyId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Company not found in tenant context"));
    }

    private UUID requireCompanyId() {
        return CompanyContext.getCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context is missing"));
    }

    private String rootMessage(Exception ex) {
        var message = ex.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        var cause = ex.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }
        return "Unexpected row processing error";
    }

    private record RawUploadRow(
            int rowNumber,
            String employeeId,
            String firstName,
            String lastName,
            String email,
            Role role,
            String managerEmployeeId,
            LocalDate joiningDate,
            int leaveBalance) {
    }
}
