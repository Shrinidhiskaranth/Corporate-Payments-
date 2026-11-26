package com.nmamit.payments;

import com.nmamit.payments.model.entity.Department;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.Role;
import com.nmamit.payments.repository.DepartmentRepository;
import com.nmamit.payments.repository.UserRepository;
import com.nmamit.payments.service.AdminService;
import com.nmamit.payments.service.NotAllowedException;
import com.nmamit.payments.service.WorkflowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AdminServiceTests {

    @Autowired AdminService adminService;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired PasswordEncoder passwordEncoder;

    User admin, employee;
    Department it;

    @BeforeEach
    void setUp() {
        admin = userRepository.findWithDepartmentByEmail("admin@corp.com").orElseThrow();
        employee = userRepository.findWithDepartmentByEmail("employee@corp.com").orElseThrow();
        it = departmentRepository.findByName("Information Technology").orElseThrow();
    }

    @Test
    void createUserStoresHashedPassword() {
        User u = adminService.createUser(admin, "Ravi Kumar", "Ravi.Kumar@corp.com", "9876543210",
                Role.EMPLOYEE, it.getId(), "secret123");
        assertThat(u.getEmail()).isEqualTo("ravi.kumar@corp.com");
        assertThat(u.getPassword()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", u.getPassword())).isTrue();
    }

    @Test
    void duplicateEmailIsRefused() {
        assertThatThrownBy(() -> adminService.createUser(admin, "Another Esha", "employee@corp.com",
                "9876543210", Role.EMPLOYEE, it.getId(), "secret123"))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void invalidMobileIsRefused() {
        assertThatThrownBy(() -> adminService.createUser(admin, "Ravi Kumar", "ravi@corp.com",
                "12345", Role.EMPLOYEE, it.getId(), "secret123"))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("10-digit");
    }

    @Test
    void adminCannotDeactivateSelf() {
        assertThatThrownBy(() -> adminService.setActive(admin, admin.getId(), false))
                .isInstanceOf(WorkflowException.class);
    }

    @Test
    void deactivateAndActivateUser() {
        adminService.setActive(admin, employee.getId(), false);
        assertThat(userRepository.findById(employee.getId()).orElseThrow().isActive()).isFalse();
        adminService.setActive(admin, employee.getId(), true);
        assertThat(userRepository.findById(employee.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void nonAdminCannotManageUsers() {
        assertThatThrownBy(() -> adminService.createDepartment(employee, "Sales", new BigDecimal("100000")))
                .isInstanceOf(NotAllowedException.class);
    }

    @Test
    void createDepartmentAndSeeItInList() {
        adminService.createDepartment(admin, "Sales", new BigDecimal("300000"));
        assertThat(adminService.departments())
                .extracting(Department::getName)
                .contains("Sales");
    }
}
