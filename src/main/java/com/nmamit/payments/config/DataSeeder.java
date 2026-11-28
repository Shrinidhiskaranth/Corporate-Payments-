package com.nmamit.payments.config;

import com.nmamit.payments.model.entity.Department;
import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.PaymentCategory;
import com.nmamit.payments.model.enums.Role;
import com.nmamit.payments.repository.DepartmentRepository;
import com.nmamit.payments.repository.PaymentRequestRepository;
import com.nmamit.payments.repository.UserRepository;
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
    private final boolean seedSampleRequests;

    public DataSeeder(DepartmentRepository departmentRepository, UserRepository userRepository,
                      PaymentRequestRepository requestRepository, PasswordEncoder passwordEncoder,
                      PaymentRequestService requestService,
                      @Value("${app.seed.sample-requests:true}") boolean seedSampleRequests) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.passwordEncoder = passwordEncoder;
        this.requestService = requestService;
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
        user("Manoj Manager", "manager@corp.com", "9000000003", Role.MANAGER, it);
        User itEmployee = user("Vinayak", "employee@corp.com", "8088966975", Role.EMPLOYEE, it);
        user("Harish HR Manager", "manager.hr@corp.com", "9000000005", Role.MANAGER, hr);
        User hrEmployee = user("Neha HR Employee", "employee.hr@corp.com", "9000000006", Role.EMPLOYEE, hr);

        if (seedSampleRequests && requestRepository.count() == 0) {
            seedRequests(itEmployee, hrEmployee);
            log.info("Sample payment requests created");
        }
    }

    private void seedRequests(User itEmployee, User hrEmployee) {
        // submitted, waiting for the manager
        submit(itEmployee, draft(itEmployee, "Antivirus licences - 50 seats", "72000",
                PaymentCategory.IT_SERVICES, "SecureSoft Pvt Ltd", "50200012345678"));
        submit(itEmployee, draft(itEmployee, "Monitors for the support team", "35600",
                PaymentCategory.IT_SERVICES, "Prime Computers", "001105012345"));
        submit(hrEmployee, draft(hrEmployee, "Leadership workshop - 2 days", "38000",
                PaymentCategory.TRAINING, "SkillUp Academy LLP", "38291045678"));

        // drafts, not submitted yet
        draft(itEmployee, "Team offsite - travel", "18000", PaymentCategory.LOGISTICS,
                "Swift Travels", "77012078901234");
        draft(hrEmployee, "Office plants - annual upkeep", "9000", PaymentCategory.FACILITY_MANAGEMENT,
                "Green Corner", "10123067890");

        // cancelled by the employee
        PaymentRequest ads = draft(hrEmployee, "Premium job posting plan", "25000",
                PaymentCategory.MARKETING, "JobLink India Pvt Ltd", "60221056789012");
        requestService.cancel(ads.getId(), hrEmployee);
    }

    private void submit(User employee, PaymentRequest request) {
        requestService.submit(request.getId(), employee);
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
