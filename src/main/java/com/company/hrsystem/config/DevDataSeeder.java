package com.company.hrsystem.config;

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
import com.company.hrsystem.leave.service.LeaveEntitlementService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("test")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private static final String DEFAULT_PASSWORD = "ChangeMe@123";
    private static final String HR_EMPLOYEE_CODE = "HR001";
    private static final String HR_USERNAME = "hr.user";

    private static final int MANAGER_COUNT = 3;
    private static final int EMPLOYEES_PER_MANAGER = 10;

    private static final List<String> FIRST_NAMES = List.of(
            "Aarav", "Vivaan", "Aditya", "Krishna", "Ishaan", "Rahul", "Ananya", "Diya", "Kavya", "Ira",
            "Arjun", "Riya", "Neha", "Karan", "Meera", "Nikhil", "Saanvi", "Pooja", "Rohan", "Priya");

    private static final List<String> LAST_NAMES = List.of(
            "Sharma", "Verma", "Singh", "Gupta", "Patel", "Reddy", "Iyer", "Kapoor", "Mishra", "Agarwal",
            "Nair", "Jain", "Yadav", "Bansal", "Kumar", "Saxena", "Das", "Mehta", "Pillai", "Rao");

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final AuthUserService authUserService;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveEntitlementService leaveEntitlementService;

    @Override
    @Transactional
    public void run(String... args) {
        if (employeeRepository.count() > 0) {
            log.info("Test employees already exist, skipping seeding.");
            return;
        }

        log.info("Generating test employee data...");

        var random = ThreadLocalRandom.current();
        var company = getOrCreateDevCompany();
        var tenantId = company.getCompanyId();
        var leaveTypes = leaveEntitlementService.ensureDefaultLeaveTypes(tenantId);

        int insertedEmployees = 0;

        var hr = createEmployee(
                company,
                tenantId,
                HR_EMPLOYEE_CODE,
                "HR",
                "Admin",
                "hr@company.com",
                null,
                randomLeaveBalance(random),
                randomJoiningDate(random));
        authUserService.createUser(tenantId, HR_USERNAME, DEFAULT_PASSWORD, Role.HR, hr.getId(), true);
        leaveEntitlementService.ensureDefaultBalancesForEmployee(tenantId, hr.getId(), LocalDate.now().getYear());
        insertedEmployees++;

        var managers = new ArrayList<Employee>(MANAGER_COUNT);
        for (int i = 1; i <= MANAGER_COUNT; i++) {
            var managerCode = String.format("MGR%03d", i);
            var manager = createEmployee(
                    company,
                    tenantId,
                    managerCode,
                    randomFirstName(random),
                    randomLastName(random),
                    "mgr" + i + "@company.com",
                    hr.getId(),
                    randomLeaveBalance(random),
                    randomJoiningDate(random));
            authUserService.createUser(
                    tenantId,
                    managerCode.toLowerCase(Locale.ROOT),
                    DEFAULT_PASSWORD,
                    Role.EMPLOYEE,
                    manager.getId(),
                    true);
            leaveEntitlementService.ensureDefaultBalancesForEmployee(tenantId, manager.getId(), LocalDate.now().getYear());
            managers.add(manager);
            insertedEmployees++;
        }

        int employeeIndex = 1;
        for (var manager : managers) {
            for (int i = 0; i < EMPLOYEES_PER_MANAGER; i++) {
                var employeeCode = String.format("EMP%03d", employeeIndex);
                var employee = createEmployee(
                        company,
                        tenantId,
                        employeeCode,
                        randomFirstName(random),
                        randomLastName(random),
                        "emp" + employeeIndex + "@company.com",
                        manager.getId(),
                        randomLeaveBalance(random),
                        randomJoiningDate(random));
                authUserService.createUser(
                        tenantId,
                        employeeCode.toLowerCase(Locale.ROOT),
                        DEFAULT_PASSWORD,
                        Role.EMPLOYEE,
                        employee.getId(),
                        true);
                leaveEntitlementService.ensureDefaultBalancesForEmployee(tenantId, employee.getId(), LocalDate.now().getYear());
                insertedEmployees++;
                employeeIndex++;
            }
        }

        seedLeaveBalances(tenantId, leaveTypes.get(LeaveEntitlementService.DefaultLeaveType.ANNUAL), random);

        log.info("Successfully inserted {} test employees", insertedEmployees);
    }

    private Company getOrCreateDevCompany() {
        var existingCompanies = companyRepository.findAll();
        if (!existingCompanies.isEmpty()) {
            return existingCompanies.get(0);
        }

        var tenantId = UUID.randomUUID();
        var company = new Company();
        company.setCompanyId(tenantId);
        company.setName("TEST Tenant Company");
        company.setCode("TEST-" + tenantId.toString().substring(0, 8).toUpperCase(Locale.ROOT));
        company.setTimezone("Asia/Kolkata");
        return companyRepository.save(company);
    }

    private Employee createEmployee(
            Company company,
            UUID tenantId,
            String employeeCode,
            String firstName,
            String lastName,
            String email,
            UUID managerId,
            int leaveBalance,
            LocalDate joiningDate) {
        var employee = new Employee();
        employee.setCompanyId(tenantId);
        employee.setCompany(company);
        employee.setEmployeeCode(employeeCode);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setManagerId(managerId);
        employee.setActive(Boolean.TRUE);
        employee.setRemainingWfhBalance(Math.max(0, leaveBalance / 2));

        var saved = employeeRepository.save(employee);

        // Employee entity currently has no joiningDate column; using joining year for leave balance initialization.
        initializeEmployeeLeaveBalance(tenantId, saved.getId(), joiningDate.getYear(), leaveBalance);

        return saved;
    }

    private void initializeEmployeeLeaveBalance(UUID tenantId, UUID employeeId, int year, int leaveBalance) {
        var leaveType = leaveTypeRepository.findByCompanyIdAndNameIgnoreCase(
                        tenantId,
                        LeaveEntitlementService.DefaultLeaveType.ANNUAL.name())
                .orElseThrow(() -> new IllegalStateException("Default leave type not found for tenant"));

        leaveBalanceRepository.findByCompanyIdAndEmployeeIdAndLeaveTypeIdAndYear(
                        tenantId,
                        employeeId,
                        leaveType.getId(),
                        year)
                .orElseGet(() -> {
                    var balance = new LeaveBalance();
                    balance.setCompanyId(tenantId);
                    balance.setTenantId(tenantId);
                    balance.setEmployeeId(employeeId);
                    balance.setLeaveTypeId(leaveType.getId());
                    balance.setYear(year);
                    balance.setAllocated(leaveBalance);
                    balance.setUsed(0);
                    balance.setRemaining(leaveBalance);
                    return leaveBalanceRepository.save(balance);
                });
    }

    private void seedLeaveBalances(UUID tenantId, LeaveType leaveType, ThreadLocalRandom random) {
        int currentYear = LocalDate.now().getYear();
        var allEmployees = employeeRepository.findByCompanyId(tenantId);
        for (var employee : allEmployees) {
            leaveBalanceRepository.findByCompanyIdAndEmployeeIdAndLeaveTypeIdAndYear(
                            tenantId,
                            employee.getId(),
                            leaveType.getId(),
                            currentYear)
                    .orElseGet(() -> {
                        int leaveBalance = randomLeaveBalance(random);
                        var balance = new LeaveBalance();
                        balance.setCompanyId(tenantId);
                        balance.setTenantId(tenantId);
                        balance.setEmployeeId(employee.getId());
                        balance.setLeaveTypeId(leaveType.getId());
                        balance.setYear(currentYear);
                        balance.setAllocated(leaveBalance);
                        balance.setUsed(0);
                        balance.setRemaining(leaveBalance);
                        return leaveBalanceRepository.save(balance);
                    });
        }
    }

    private int randomLeaveBalance(ThreadLocalRandom random) {
        return random.nextInt(10, 26);
    }

    private LocalDate randomJoiningDate(ThreadLocalRandom random) {
        long daysBack = random.nextLong(0, 731);
        return LocalDate.now().minusDays(daysBack);
    }

    private String randomFirstName(ThreadLocalRandom random) {
        return FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size()));
    }

    private String randomLastName(ThreadLocalRandom random) {
        return LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));
    }
}
