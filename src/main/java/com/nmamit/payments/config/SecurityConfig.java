package com.nmamit.payments.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security setup.
 * - Users log in with email + password stored in MySQL (see CustomUserDetailsService).
 * - Passwords are checked against BCrypt hashes.
 * - Each folder of pages is restricted to one role; services check roles again.
 * - CSRF protection is on; every form includes the _csrf hidden field.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // public pages and JSF resources
                .requestMatchers("/", "/index.xhtml", "/login.xhtml",
                                 "/jakarta.faces.resource/**", "/error").permitAll()
                // role-specific areas
                .requestMatchers("/employee/**").hasRole("EMPLOYEE")
                .requestMatchers("/manager/**").hasRole("MANAGER")
                .requestMatchers("/finance/**").hasRole("FINANCE")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // everything else (dashboard, request detail) needs a logged-in user;
                // the services limit what each role can see
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login.xhtml")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard.xhtml", true)
                .failureUrl("/login.xhtml?error=true")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.xhtml?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied.xhtml"));
        return http.build();
    }
}
