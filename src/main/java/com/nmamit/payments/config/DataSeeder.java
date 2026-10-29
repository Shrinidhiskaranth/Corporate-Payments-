package com.nmamit.payments.config;

import com.nmamit.payments.model.entity.Department;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.Role;
import com.nmamit.payments.repository.DepartmentRepository;
import com.nmamit.payments.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Inserts starting departments and one user per role the first time the app runs.
 * Default password for every seeded user: password123 (stored as a BCrypt hash).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(DepartmentRepository departmentRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Department finance = department("Finance", "5000000");
        Department it = department("Information Technology", "2000000");
        department("Human Resources", "1000000");

        user("System Admin", "admin@corp.com", "9000000001", Role.ADMIN, finance);
        user("Fiona Finance", "finance@corp.com", "9000000002", Role.FINANCE, finance);
        user("Manoj Manager", "manager@corp.com", "9000000003", Role.MANAGER, it);
        user("Esha Employee", "employee@corp.com", "9000000004", Role.EMPLOYEE, it);
    }

    private Department department(String name, String budget) {
        return departmentRepository.findByName(name)
                .orElseGet(() -> departmentRepository.save(
                        new Department(null, name, new BigDecimal(budget))));
    }

    private void user(String name, String email, String mobile, Role role, Department dept) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("password123"));
        u.setMobileNumber(mobile);
        u.setRole(role);
        u.setDepartment(dept);
        userRepository.save(u);
    }
}
