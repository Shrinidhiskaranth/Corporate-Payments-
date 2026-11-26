package com.nmamit.payments.config;

import com.nmamit.payments.model.entity.Department;
import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.Decision;
import com.nmamit.payments.model.enums.PaymentCategory;
import com.nmamit.payments.model.enums.Role;
import com.nmamit.payments.repository.DepartmentRepository;
import com.nmamit.payments.repository.PaymentRequestRepository;
import com.nmamit.payments.repository.UserRepository;
import com.nmamit.payments.service.ApprovalService;
import com.nmamit.payments.service.PaymentRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Inserts starting data the first time the app runs:
 * - 3 departments and 6 users (password for all: password123)
 * - if app.seed.sample-requests=true, payment requests in different statuses
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PaymentRequestRepository requestRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaymentRequestService requestService;
    private final ApprovalService approvalService;
    private final boolean seedSampleRequests;

    public DataSeeder(DepartmentRepository departmentRepository, UserRepository userRepository,
                      PaymentRequestRepository requestRepository, PasswordEncoder passwordEncoder,
                      PaymentRequestService requestService, ApprovalService approvalService,
                      @Value("${app.seed.sample-requests:true}") boolean seedSampleRequests) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.passwordEncoder = passwordEncoder;
        this.requestService = requestService;
        this.approvalService = approvalService;
        this.seedSampleRequests = seedSampleRequests;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Department finance = department("Finance", "5000000");
        Department it = department("Information Technology", "2000000");
        Department hr = department("Human Resources", "1000000");

        user("System Admin", "admin@corp.com", "9000000001", Role.ADMIN, finance);
        user("Jayesh", "mvinayakrkamath@gmail.com", "8971205014", Role.FINANCE, finance);
        User itManager = user("Manoj Manager", "manager@corp.com", "9000000003", Role.MANAGER, it);
        User itEmployee = user("Vinayak", "employee@corp.com", "8088966975", Role.EMPLOYEE, it);
        User hrManager = user("Harish HR Manager", "manager.hr@corp.com", "9000000005", Role.MANAGER, hr);
        User hrEmployee = user("Neha HR Employee", "employee.hr@corp.com", "9000000006", Role.EMPLOYEE, hr);

        if (seedSampleRequests && requestRepository.count() == 0) {
            seedRequests(itManager, itEmployee, hrManager, hrEmployee);
            log.info("Sample payment requests created");
        }
    }

    private void seedRequests(User itManager, User itEmployee, User hrManager, User hrEmployee) {
        // 1. Approved by the manager
        PaymentRequest antivirus = draft(itEmployee, "Antivirus licences - 50 seats", "72000",
                PaymentCategory.IT_SERVICES, "SecureSoft Pvt Ltd", "50200012345678");
        requestService.submit(antivirus.getId(), itEmployee);
        approvalService.managerDecision(antivirus.getId(), itManager, Decision.APPROVED, "Renewal is due");

        // 2. Waiting for the manager
        PaymentRequest monitors = draft(itEmployee, "Monitors for the support team", "35600",
                PaymentCategory.IT_SERVICES, "Prime Computers", "001105012345");
        requestService.submit(monitors.getId(), itEmployee);

        // 3. Waiting for the manager
        PaymentRequest cables = draft(itEmployee, "Network cables and connectors", "5500",
                PaymentCategory.MAINTENANCE, "Cable Mart", "912010034567890");
        requestService.submit(cables.getId(), itEmployee);

        // 4. Draft, not submitted yet
        draft(itEmployee, "Team offsite - travel", "18000", PaymentCategory.LOGISTICS,
                "Swift Travels", "77012078901234");

        // 5. Rejected by the HR manager
        PaymentRequest ads = draft(hrEmployee, "Premium job posting plan", "25000",
                PaymentCategory.MARKETING, "JobLink India Pvt Ltd", "60221056789012");
        requestService.submit(ads.getId(), hrEmployee);
        approvalService.managerDecision(ads.getId(), hrManager, Decision.REJECTED,
                "The free plan meets current hiring needs");

        // 6. Approved by the HR manager
        PaymentRequest training = draft(hrEmployee, "Leadership workshop - 2 days", "38000",
                PaymentCategory.TRAINING, "SkillUp Academy LLP", "38291045678");
        requestService.submit(training.getId(), hrEmployee);
        approvalService.managerDecision(training.getId(), hrManager, Decision.APPROVED, "Planned in training calendar");

        // 7. Large request, approved by the manager
        PaymentRequest servers = draft(itEmployee, "Server upgrade - 2 rack servers", "210000",
                PaymentCategory.IT_SERVICES, "RackTech Systems", "10123067890");
        requestService.submit(servers.getId(), itEmployee);
        approvalService.managerDecision(servers.getId(), itManager, Decision.APPROVED, "Current servers out of warranty");

        // 8. Waiting for the manager
        PaymentRequest plugin = draft(itEmployee, "IDE plugin subscription", "6500",
                PaymentCategory.IT_SERVICES, "DevTools Pvt Ltd", "50100098765432");
        requestService.submit(plugin.getId(), itEmployee);
    }

    private PaymentRequest draft(User employee, String title, String amount, PaymentCategory category,
                                 String beneficiary, String account) {
        return requestService.createDraft(employee, new PaymentRequestService.RequestForm(title, null,
                new BigDecimal(amount), category, beneficiary, account, LocalDate.now().plusDays(15)));
    }

    private Department department(String name, String budget) {
        return departmentRepository.findByName(name)
                .orElseGet(() -> departmentRepository.save(new Department(null, name, new BigDecimal(budget))));
    }

    private User user(String name, String email, String mobile, Role role, Department dept) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setFullName(name);
            u.setEmail(email);
            u.setPassword(passwordEncoder.encode("password123"));
            u.setMobileNumber(mobile);
            u.setRole(role);
            u.setDepartment(dept);
            return userRepository.save(u);
        });
    }
}
