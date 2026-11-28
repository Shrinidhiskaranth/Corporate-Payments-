package com.nmamit.payments;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTests {

    @Autowired MockMvc mockMvc;

    @Test
    void validLoginSucceedsWithEmployeeRole() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .userParameter("email").user("employee@corp.com")
                        .passwordParam("password").password("password123"))
                .andExpect(authenticated().withRoles("EMPLOYEE"))
                .andExpect(redirectedUrl("/dashboard.xhtml"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .userParameter("email").user("employee@corp.com")
                        .passwordParam("password").password("wrong"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login.xhtml?error=true"));
    }

    @Test
    void dashboardRequiresLogin() throws Exception {
        mockMvc.perform(get("/dashboard.xhtml"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login.xhtml"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotOpenManagerPages() throws Exception {
        mockMvc.perform(get("/manager/approvals.xhtml"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotOpenFinancePages() throws Exception {
        mockMvc.perform(get("/finance/approvals.xhtml"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    void financeCannotOpenAdminPages() throws Exception {
        mockMvc.perform(get("/admin/users.xhtml"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotOpenFinancePages() throws Exception {
        mockMvc.perform(get("/finance/approvals.xhtml"))
                .andExpect(status().isForbidden());
    }
}
