package com.company.hrsystem;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.hrsystem.auth.entity.Role;
import com.company.hrsystem.auth.service.AuthUserService;
import com.company.hrsystem.company.entity.Company;
import com.company.hrsystem.company.repository.CompanyRepository;
import com.company.hrsystem.employee.entity.Employee;
import com.company.hrsystem.employee.repository.EmployeeRepository;
import com.company.hrsystem.leave.entity.LeaveBalance;
import com.company.hrsystem.leave.entity.LeaveType;
import com.company.hrsystem.leave.repository.LeaveBalanceRepository;
import com.company.hrsystem.leave.repository.LeaveTypeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ApiEndToEndIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private AuthUserService authUserService;

    private UUID tenantId;
    private Employee managerEmployee;
    private Employee regularEmployee;
    private LeaveType annualLeaveType;

    @BeforeEach
    void setUpTenantData() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();

        tenantId = UUID.randomUUID();

        var company = new Company();
        company.setCompanyId(tenantId);
        company.setName("Acme HQ");
        company.setCode("ACME-" + tenantId.toString().substring(0, 8));
        company.setTimezone("UTC");
        var savedCompany = companyRepository.save(company);

        managerEmployee = new Employee();
        managerEmployee.setCompanyId(tenantId);
        managerEmployee.setCompany(savedCompany);
        managerEmployee.setEmployeeCode("HRMGR");
        managerEmployee.setFirstName("Helen");
        managerEmployee.setLastName("Manager");
        managerEmployee.setEmail("helen.manager+" + tenantId + "@example.com");
        managerEmployee.setActive(Boolean.TRUE);
        managerEmployee.setRemainingWfhBalance(10);
        managerEmployee = employeeRepository.save(managerEmployee);

        regularEmployee = new Employee();
        regularEmployee.setCompanyId(tenantId);
        regularEmployee.setCompany(savedCompany);
        regularEmployee.setEmployeeCode("EMP001");
        regularEmployee.setFirstName("Ethan");
        regularEmployee.setLastName("Worker");
        regularEmployee.setEmail("ethan.worker+" + tenantId + "@example.com");
        regularEmployee.setManagerId(managerEmployee.getId());
        regularEmployee.setActive(Boolean.TRUE);
        regularEmployee.setRemainingWfhBalance(6);
        regularEmployee = employeeRepository.save(regularEmployee);

        annualLeaveType = new LeaveType();
        annualLeaveType.setCompanyId(tenantId);
        annualLeaveType.setTenantId(tenantId);
        annualLeaveType.setName("ANNUAL");
        annualLeaveType.setAnnualQuota(24);
        annualLeaveType.setCarryForwardAllowed(Boolean.TRUE);
        annualLeaveType = leaveTypeRepository.save(annualLeaveType);

        var leaveBalance = new LeaveBalance();
        leaveBalance.setCompanyId(tenantId);
        leaveBalance.setTenantId(tenantId);
        leaveBalance.setEmployeeId(regularEmployee.getId());
        leaveBalance.setLeaveTypeId(annualLeaveType.getId());
        leaveBalance.setYear(LocalDate.now().getYear());
        leaveBalance.setAllocated(24);
        leaveBalance.setUsed(0);
        leaveBalance.setRemaining(24);
        leaveBalanceRepository.save(leaveBalance);

        authUserService.createUser(tenantId, "superadmin", "password", Role.SUPER_ADMIN, null, true);
        authUserService.createUser(tenantId, "hrmanager", "password", Role.HR, managerEmployee.getId(), true);
        authUserService.createUser(tenantId, "employee1", "password", Role.EMPLOYEE, regularEmployee.getId(), true);
    }

    @Test
    void shouldExerciseAllApisEndToEnd() throws Exception {
        var superAdminToken = loginAndGetToken("superadmin", "password");
        var hrToken = loginAndGetToken("hrmanager", "password");
        var employeeToken = loginAndGetToken("employee1", "password");

        var createdCompany = performJson(
                withAuthAndJson(
                        post("/api/v1/admin/companies"),
                        superAdminToken,
                        Map.of("name", "Regional Office", "code", "REG-" + UUID.randomUUID(), "timezone", "UTC")),
                200);
        var createdCompanyId = UUID.fromString(createdCompany.get("id").asText());

        performJson(withAuth(get("/api/v1/admin/companies/{companyId}", createdCompanyId), superAdminToken), 200);
        performJson(
                withAuthAndJson(
                        put("/api/v1/admin/companies/{companyId}", createdCompanyId),
                        superAdminToken,
                        Map.of("name", "Regional Office Updated", "code", "REG-UPD-" + UUID.randomUUID(), "timezone", "UTC")),
                200);

        var createdShift = performJson(
                withAuthAndJson(
                        post("/api/v1/admin/shifts"),
                        hrToken,
                        Map.of(
                                "shiftName", "General",
                                "startTime", LocalTime.of(9, 0).toString(),
                                "endTime", LocalTime.of(18, 0).toString(),
                                "graceMinutes", 10,
                                "minimumHalfDayMinutes", 240,
                                "minimumFullDayMinutes", 480)),
                200);
        var shiftId = UUID.fromString(createdShift.get("id").asText());

        performJson(withAuth(get("/api/v1/admin/shifts"), hrToken), 200);
        performJson(
                withAuthAndJson(
                        put("/api/v1/admin/shifts/{shiftId}", shiftId),
                        hrToken,
                        Map.of(
                                "shiftName", "General Updated",
                                "startTime", LocalTime.of(9, 30).toString(),
                                "endTime", LocalTime.of(18, 30).toString(),
                                "graceMinutes", 15,
                                "minimumHalfDayMinutes", 240,
                                "minimumFullDayMinutes", 480)),
                200);

        var temporaryShift = performJson(
                withAuthAndJson(
                        post("/api/v1/admin/shifts"),
                        hrToken,
                        Map.of(
                                "shiftName", "Temp Delete",
                                "startTime", LocalTime.of(8, 0).toString(),
                                "endTime", LocalTime.of(17, 0).toString(),
                                "graceMinutes", 5,
                                "minimumHalfDayMinutes", 200,
                                "minimumFullDayMinutes", 450)),
                200);
        var shiftToDelete = UUID.fromString(temporaryShift.get("id").asText());
        mockMvc.perform(withAuth(delete("/api/v1/admin/shifts/{shiftId}", shiftToDelete), hrToken))
                .andExpect(status().isNoContent());

        var createdEmployee = performJson(
                withAuthAndJson(
                        post("/api/v1/admin/employees"),
                        hrToken,
                        Map.of(
                                "employeeCode", "EMP900",
                                "firstName", "Test",
                                "lastName", "User",
                                "email", "test.user+" + UUID.randomUUID() + "@example.com",
                                "shiftId", shiftId,
                                "managerId", managerEmployee.getId(),
                                "remainingWfhBalance", 2)),
                200);
        var adminCreatedEmployeeId = UUID.fromString(createdEmployee.get("id").asText());

        performJson(withAuth(get("/api/v1/admin/employees"), hrToken), 200);
        performJson(
                withAuthAndJson(
                        put("/api/v1/admin/employees/{employeeId}", adminCreatedEmployeeId),
                        hrToken,
                        Map.of(
                                "employeeCode", "EMP900-UPD",
                                "firstName", "TestUpdated",
                                "lastName", "UserUpdated",
                                "email", "test.updated+" + UUID.randomUUID() + "@example.com",
                                "shiftId", shiftId,
                                "managerId", managerEmployee.getId(),
                                "active", true,
                                "remainingWfhBalance", 4)),
                200);
        performJson(withAuth(patch("/api/v1/admin/employees/{employeeId}/deactivate", adminCreatedEmployeeId), hrToken), 200);

        performJson(withAuth(get("/api/v1/admin/work-policy"), hrToken), 200);
        performJson(
                withAuthAndJson(
                        put("/api/v1/admin/work-policy"),
                        hrToken,
                        Map.of(
                                "allowedWfhPerMonth", 4,
                                "autoDeduct", true,
                                "minimumWorkingMinutes", 480,
                                "halfDayAllowed", true,
                                "halfDayThresholdMinutes", 240)),
                200);

        var createdHoliday = performJson(
                withAuthAndJson(
                        post("/api/v1/admin/holidays"),
                        hrToken,
                        Map.of("date", LocalDate.now().plusDays(20).toString(), "name", "Founders Day")),
                200);
        var holidayId = UUID.fromString(createdHoliday.get("id").asText());

        performJson(
                withAuth(get(
                                "/api/v1/admin/holidays?from={from}&to={to}",
                                LocalDate.now().plusDays(19),
                                LocalDate.now().plusDays(21)),
                        hrToken),
                200);
        performJson(
                withAuthAndJson(
                        put("/api/v1/admin/holidays/{holidayId}", holidayId),
                        hrToken,
                        Map.of("date", LocalDate.now().plusDays(21).toString(), "name", "Founders Day Updated")),
                200);
        mockMvc.perform(withAuth(delete("/api/v1/admin/holidays/{holidayId}", holidayId), hrToken))
                .andExpect(status().isNoContent());

        var createdLeaveType = performJson(
                withAuthAndJson(
                        post("/api/v1/admin/leave-types"),
                        hrToken,
                        Map.of("name", "SABBATICAL", "annualQuota", 10, "carryForwardAllowed", false)),
                200);
        var leaveTypeId = UUID.fromString(createdLeaveType.get("id").asText());
        performJson(withAuth(get("/api/v1/admin/leave-types"), hrToken), 200);
        performJson(
                withAuthAndJson(
                        put("/api/v1/admin/leave-types/{leaveTypeId}", leaveTypeId),
                        hrToken,
                        Map.of("name", "SABBATICAL_PLUS", "annualQuota", 12, "carryForwardAllowed", true)),
                200);
        mockMvc.perform(withAuth(delete("/api/v1/admin/leave-types/{leaveTypeId}", leaveTypeId), hrToken))
                .andExpect(status().isNoContent());

        var createdBalance = performJson(
                withAuthAndJson(
                        post("/api/v1/admin/leave-balances"),
                        hrToken,
                        Map.of(
                                "employeeId", adminCreatedEmployeeId,
                                "leaveTypeId", annualLeaveType.getId(),
                                "year", LocalDate.now().getYear(),
                                "allocated", 10,
                                "used", 2)),
                200);
        var leaveBalanceId = UUID.fromString(createdBalance.get("id").asText());
        performJson(withAuth(get("/api/v1/admin/leave-balances?employeeId={employeeId}", adminCreatedEmployeeId), hrToken), 200);
        performJson(
                withAuthAndJson(
                        put("/api/v1/admin/leave-balances/{balanceId}", leaveBalanceId),
                        hrToken,
                        Map.of("allocated", 12, "used", 3)),
                200);
        mockMvc.perform(withAuth(delete("/api/v1/admin/leave-balances/{balanceId}", leaveBalanceId), hrToken))
                .andExpect(status().isNoContent());

        performJson(
                withAuthAndJson(
                        post("/api/v1/attendance/check-in"),
                        employeeToken,
                        Map.of("employeeId", regularEmployee.getId())),
                200);
        performJson(
                withAuthAndJson(
                        post("/api/v1/attendance/check-out"),
                        employeeToken,
                        Map.of("employeeId", regularEmployee.getId())),
                200);
        performJson(withAuth(get("/api/v1/attendance/me?from={from}&to={to}", LocalDate.now(), LocalDate.now()), employeeToken), 200);
        performJson(withAuth(get("/api/v1/attendance/self?from={from}", LocalDate.now()), employeeToken), 200);

        performJson(
                withAuthAndJson(
                        post("/api/v1/biometric/events"),
                        hrToken,
                        Map.of(
                                "deviceId", "DEVICE-1",
                                "employeeCode", regularEmployee.getEmployeeCode(),
                                "eventType", "CHECK_IN",
                                "eventTime", java.time.LocalDateTime.now().toString(),
                                "deviceLogId", "BIO-" + UUID.randomUUID())),
                200);

        var firstLeaveDate = LocalDate.now().plusDays(5);
        var secondLeaveDate = LocalDate.now().plusDays(6);
        var thirdLeaveDate = LocalDate.now().plusDays(7);
        var fourthLeaveDate = LocalDate.now().plusDays(8);
        var fifthLeaveDate = LocalDate.now().plusDays(9);
        var sixthLeaveDate = LocalDate.now().plusDays(10);

        var leaveApply1 = performJson(
                withAuthAndJson(
                        post("/leave/apply"),
                        employeeToken,
                        Map.of(
                                "leaveType", "ANNUAL",
                                "fromDate", firstLeaveDate.toString(),
                                "toDate", firstLeaveDate.toString(),
                                "reason", "Personal work")),
                200);
        var leave1Id = UUID.fromString(leaveApply1.get("id").asText());
        performJson(withAuth(get("/leave/my"), employeeToken), 200);
        performJson(withAuth(get("/leave/pending"), hrToken), 200);

        var managerNotifications = performJson(withAuth(get("/notifications/my"), hrToken), 200);
        if (managerNotifications.get("notifications").isArray()
                && !managerNotifications.get("notifications").isEmpty()) {
            var notificationId = UUID.fromString(managerNotifications.get("notifications").get(0).get("id").asText());
            mockMvc.perform(withAuth(put("/notifications/{notificationId}/read", notificationId), hrToken))
                    .andExpect(status().isNoContent());
        }

        performJson(
                withAuthAndJson(
                        post("/leave/{leaveRequestId}/approve", leave1Id),
                        hrToken,
                        Map.of("remarks", "Approved")),
                200);

        var leaveApply2 = performJson(
                withAuthAndJson(
                        post("/leave/apply"),
                        employeeToken,
                        Map.of(
                                "leaveType", "ANNUAL",
                                "fromDate", secondLeaveDate.toString(),
                                "toDate", secondLeaveDate.toString(),
                                "reason", "Medical")),
                200);
        var leave2Id = UUID.fromString(leaveApply2.get("id").asText());
        performJson(
                withAuthAndJson(
                        post("/leave/{leaveRequestId}/reject", leave2Id),
                        hrToken,
                        Map.of("remarks", "Insufficient staffing")),
                200);

        var leaveApply3 = performJson(
                withAuthAndJson(
                        post("/leave/apply"),
                        employeeToken,
                        Map.of(
                                "leaveType", "ANNUAL",
                                "fromDate", thirdLeaveDate.toString(),
                                "toDate", thirdLeaveDate.toString(),
                                "reason", "Family event")),
                200);
        var leave3Id = UUID.fromString(leaveApply3.get("id").asText());
        performJson(withAuth(patch("/leave/approve/{leaveRequestId}", leave3Id), hrToken), 200);

        var leaveApply4 = performJson(
                withAuthAndJson(
                        post("/leave/apply"),
                        employeeToken,
                        Map.of(
                                "leaveType", "ANNUAL",
                                "fromDate", fourthLeaveDate.toString(),
                                "toDate", fourthLeaveDate.toString(),
                                "reason", "Cancelable request")),
                200);
        var leave4Id = UUID.fromString(leaveApply4.get("id").asText());
        performJson(withAuth(patch("/api/v1/admin/leaves/{leaveRequestId}/cancel", leave4Id), employeeToken), 200);

        var adminLeave1 = performJson(
                withAuthAndJson(
                        post("/api/v1/admin/leaves"),
                        hrToken,
                        Map.of(
                                "employeeId", regularEmployee.getId(),
                                "fromDate", fifthLeaveDate.toString(),
                                "toDate", fifthLeaveDate.toString(),
                                "leaveType", "ANNUAL",
                                "reason", "Admin-created")),
                200);
        var adminLeave1Id = UUID.fromString(adminLeave1.get("id").asText());
        performJson(withAuth(patch("/api/v1/admin/leaves/{leaveRequestId}/approve", adminLeave1Id), hrToken), 200);

        var adminLeave2 = performJson(
                withAuthAndJson(
                        post("/api/v1/admin/leaves"),
                        hrToken,
                        Map.of(
                                "employeeId", regularEmployee.getId(),
                                "fromDate", sixthLeaveDate.toString(),
                                "toDate", sixthLeaveDate.toString(),
                                "leaveType", "ANNUAL",
                                "reason", "Admin-created reject")),
                200);
        var adminLeave2Id = UUID.fromString(adminLeave2.get("id").asText());
        performJson(withAuth(patch("/api/v1/admin/leaves/{leaveRequestId}/reject", adminLeave2Id), hrToken), 200);
        performJson(withAuth(get("/api/v1/admin/leaves"), hrToken), 200);

        performJson(withAuth(get("/notifications/my"), employeeToken), 200);

        var bulkUploadMvcResult = mockMvc.perform(
                        multipart("/api/admin/users/bulk-upload")
                                .file(buildBulkUploadFile())
                                .header(AUTHORIZATION, "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andReturn();
        var bulkUploadResponse = objectMapper.readTree(bulkUploadMvcResult.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(1, bulkUploadResponse.get("successCount").asInt());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        var loginResponse = performJson(
                post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenantId", tenantId,
                                "username", username,
                                "password", password))),
                200);
        return loginResponse.get("accessToken").asText();
    }

    private MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder builder, String token) {
        return builder
                .header(AUTHORIZATION, "Bearer " + token)
                .accept(APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder withAuthAndJson(
            MockHttpServletRequestBuilder builder,
            String token,
            Object body) throws Exception {
        return withAuth(builder, token)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().is(expectedStatus))
                .andReturn();
        var content = result.getResponse().getContentAsString();
        if (content == null || content.isBlank()) {
            return objectMapper.readTree("{}");
        }
        return objectMapper.readTree(content);
    }

    private MockMultipartFile buildBulkUploadFile() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Employees");
            var headers = List.of(
                    "employeeId",
                    "firstName",
                    "lastName",
                    "email",
                    "role",
                    "managerEmployeeId",
                    "joiningDate",
                    "leaveBalance");
            var headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }

            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("BULK-" + UUID.randomUUID().toString().substring(0, 8));
            row.createCell(1).setCellValue("Bulk");
            row.createCell(2).setCellValue("User");
            row.createCell(3).setCellValue("bulk.user+" + UUID.randomUUID() + "@example.com");
            row.createCell(4).setCellValue("EMPLOYEE");
            row.createCell(5).setCellValue(managerEmployee.getEmployeeCode());
            row.createCell(6).setCellValue(LocalDate.now().toString());
            row.createCell(7).setCellValue("5");

            var out = new ByteArrayOutputStream();
            workbook.write(out);
            return new MockMultipartFile(
                    "file",
                    "employees.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }
}
