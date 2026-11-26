package com.nmamit.payments.service;

import com.nmamit.payments.model.entity.AuditLog;
import com.nmamit.payments.model.entity.Department;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.AuditAction;
import com.nmamit.payments.model.enums.Role;
import com.nmamit.payments.repository.DepartmentRepository;
import com.nmamit.payments.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** User and department management for administrators. */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AdminService(UserRepository userRepository, DepartmentRepository departmentRepository,
                        PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<User> users() {
        return userRepository.findAllByOrderByFullNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Department> departments() {
        return departmentRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public User createUser(User admin, String fullName, String email, String mobile, Role role,
                           Long departmentId, String rawPassword) {
        requireAdmin(admin);
        if (fullName == null || fullName.trim().length() < 3) {
            throw new WorkflowException("Full name must have at least 3 characters");
        }
        if (email == null || !email.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new WorkflowException("Enter a valid email address");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new WorkflowException("A user with email " + normalizedEmail + " already exists");
        }
        if (mobile == null || !mobile.trim().matches("[6-9]\\d{9}")) {
            throw new WorkflowException("Mobile number must be a 10-digit Indian number starting with 6-9");
        }
        if (role == null) {
            throw new WorkflowException("Choose a role");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new WorkflowException("Password must have at least 8 characters");
        }
        Department dept = null;
        if (departmentId != null) {
            dept = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new WorkflowException("Department not found"));
        }
        if ((role == Role.EMPLOYEE || role == Role.MANAGER) && dept == null) {
            throw new WorkflowException("Employees and managers must belong to a department");
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setMobileNumber(mobile.trim());
        user.setRole(role);
        user.setDepartment(dept);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setActive(true);
        userRepository.save(user);

        auditService.log(admin, AuditAction.CREATE_USER, AuditLog.TYPE_USER, user.getId(),
                "Created user " + normalizedEmail + " with role " + role);
        return user;
    }

    @Transactional
    public User setActive(User admin, Long userId, boolean active) {
        requireAdmin(admin);
        if (Objects.equals(admin.getId(), userId) && !active) {
            throw new WorkflowException("You cannot deactivate your own account");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WorkflowException("User not found"));
        user.setActive(active);
        auditService.log(admin, AuditAction.UPDATE_USER, AuditLog.TYPE_USER, user.getId(),
                (active ? "Activated " : "Deactivated ") + user.getEmail());
        return user;
    }

    @Transactional
    public Department createDepartment(User admin, String name, BigDecimal budgetLimit) {
        requireAdmin(admin);
        if (name == null || name.trim().length() < 2) {
            throw new WorkflowException("Department name must have at least 2 characters");
        }
        if (departmentRepository.existsByNameIgnoreCase(name.trim())) {
            throw new WorkflowException("Department " + name.trim() + " already exists");
        }
        if (budgetLimit != null && budgetLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WorkflowException("Budget limit must be greater than zero (or left empty)");
        }
        Department dept = departmentRepository.save(new Department(null, name.trim(), budgetLimit));
        auditService.log(admin, AuditAction.CREATE_DEPARTMENT, AuditLog.TYPE_DEPARTMENT, dept.getId(),
                "Created department " + dept.getName());
        return dept;
    }

    private static void requireAdmin(User user) {
        if (user.getRole() != Role.ADMIN) {
            throw new NotAllowedException("Only administrators can do this");
        }
    }
}
