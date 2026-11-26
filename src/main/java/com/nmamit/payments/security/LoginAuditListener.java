package com.nmamit.payments.security;

import com.nmamit.payments.model.entity.AuditLog;
import com.nmamit.payments.model.enums.AuditAction;
import com.nmamit.payments.repository.UserRepository;
import com.nmamit.payments.service.AuditService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Writes a LOGIN row to the audit log every time a user signs in successfully. */
@Component
public class LoginAuditListener {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public LoginAuditListener(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(user ->
                auditService.log(user, AuditAction.LOGIN, AuditLog.TYPE_USER, user.getId(), "User logged in"));
    }
}
