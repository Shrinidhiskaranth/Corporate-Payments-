package com.nmamit.payments.bean;

import com.nmamit.payments.model.entity.AuditLog;
import com.nmamit.payments.model.entity.Department;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.Role;
import com.nmamit.payments.service.AdminService;
import com.nmamit.payments.service.AuditService;
import com.nmamit.payments.service.CurrentUserService;
import com.nmamit.payments.service.WorkflowException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Backing bean for admin/users.xhtml, admin/departments.xhtml and admin/audit.xhtml. */
@Component("adminBean")
@RequestScope
public class AdminBean {

    private final CurrentUserService currentUserService;
    private final AdminService adminService;
    private final AuditService auditService;

    // new user form
    @Getter @Setter private String fullName;
    @Getter @Setter private String email;
    @Getter @Setter private String mobile;
    @Getter @Setter private Role role = Role.EMPLOYEE;
    @Getter @Setter private Long departmentId;
    @Getter @Setter private String password;

    // new department form
    @Getter @Setter private String departmentName;
    @Getter @Setter private BigDecimal budgetLimit;

    private User admin;
    private List<User> users;
    private List<Department> departments;
    private List<AuditLog> auditLogs;

    public AdminBean(CurrentUserService currentUserService, AdminService adminService, AuditService auditService) {
        this.currentUserService = currentUserService;
        this.adminService = adminService;
        this.auditService = auditService;
    }

    private User admin() {
        if (admin == null) {
            admin = currentUserService.get();
        }
        return admin;
    }

    public Role[] getRoles() {
        return Role.values();
    }

    public List<User> getUsers() {
        if (users == null) {
            users = adminService.users();
        }
        return users;
    }

    public List<Department> getDepartments() {
        if (departments == null) {
            departments = adminService.departments();
        }
        return departments;
    }

    public List<AuditLog> getAuditLogs() {
        if (auditLogs == null) {
            auditLogs = auditService.latest();
        }
        return auditLogs;
    }

    public boolean isSelf(User u) {
        return Objects.equals(u.getId(), admin().getId());
    }

    public String createUser() {
        try {
            User u = adminService.createUser(admin(), fullName, email, mobile, role, departmentId, password);
            FacesUtil.infoAfterRedirect("User " + u.getEmail() + " created.");
            return "/admin/users.xhtml?faces-redirect=true";
        } catch (WorkflowException e) {
            FacesUtil.error(e.getMessage());
            return null;
        }
    }

    public String toggleActive(Long userId, boolean active) {
        try {
            User u = adminService.setActive(admin(), userId, active);
            FacesUtil.infoAfterRedirect(u.getEmail() + (active ? " activated." : " deactivated."));
        } catch (WorkflowException e) {
            FacesUtil.errorAfterRedirect(e.getMessage());
        }
        return "/admin/users.xhtml?faces-redirect=true";
    }

    public String createDepartment() {
        try {
            Department d = adminService.createDepartment(admin(), departmentName, budgetLimit);
            FacesUtil.infoAfterRedirect("Department " + d.getName() + " created.");
            return "/admin/departments.xhtml?faces-redirect=true";
        } catch (WorkflowException e) {
            FacesUtil.error(e.getMessage());
            return null;
        }
    }
}
