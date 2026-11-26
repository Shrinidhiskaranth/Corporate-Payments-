package com.nmamit.payments.service;

import com.nmamit.payments.model.entity.AuditLog;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.entity.WorkflowSettings;
import com.nmamit.payments.model.enums.ApprovalLevel;
import com.nmamit.payments.model.enums.AuditAction;
import com.nmamit.payments.model.enums.Role;
import com.nmamit.payments.repository.WorkflowSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Reads and changes the amount-based approval rules (one settings row). */
@Service
public class WorkflowSettingsService {

    private final WorkflowSettingsRepository repository;
    private final AuditService auditService;

    public WorkflowSettingsService(WorkflowSettingsRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    /** The current settings; the row is created with default values the first time. */
    @Transactional
    public WorkflowSettings get() {
        return repository.findById(WorkflowSettings.SINGLETON_ID)
                .orElseGet(() -> repository.save(new WorkflowSettings()));
    }

    /** Highest approval step needed for an amount under the current rules. */
    @Transactional
    public ApprovalLevel requiredLevel(BigDecimal amount) {
        WorkflowSettings s = get();
        if (amount.compareTo(s.getManagerOnlyLimit()) <= 0) {
            return ApprovalLevel.MANAGER;
        }
        if (amount.compareTo(s.getDirectorThreshold()) <= 0) {
            return ApprovalLevel.FINANCE;
        }
        return ApprovalLevel.DIRECTOR;
    }

    @Transactional
    public WorkflowSettings update(User admin, BigDecimal managerOnlyLimit, BigDecimal directorThreshold) {
        if (admin.getRole() != Role.ADMIN) {
            throw new NotAllowedException("Only administrators can change workflow settings");
        }
        if (managerOnlyLimit == null || managerOnlyLimit.signum() <= 0) {
            throw new WorkflowException("Manager-only limit must be greater than zero");
        }
        if (directorThreshold == null || directorThreshold.compareTo(managerOnlyLimit) <= 0) {
            throw new WorkflowException("Director threshold must be greater than the manager-only limit");
        }
        if (managerOnlyLimit.scale() > 2 || directorThreshold.scale() > 2) {
            throw new WorkflowException("Amounts can have at most 2 decimal places");
        }
        WorkflowSettings s = get();
        String before = describe(s);
        s.setManagerOnlyLimit(managerOnlyLimit);
        s.setDirectorThreshold(directorThreshold);
        auditService.log(admin, AuditAction.UPDATE_SETTINGS, AuditLog.TYPE_SETTINGS, s.getId(),
                "Workflow settings changed from [" + before + "] to [" + describe(s) + "]");
        return s;
    }

    /** Steps a request goes through when its final level is the given one, e.g. [MANAGER, FINANCE]. */
    public static List<ApprovalLevel> route(ApprovalLevel finalLevel) {
        List<ApprovalLevel> steps = new ArrayList<>();
        for (ApprovalLevel l : ApprovalLevel.values()) {
            steps.add(l);
            if (l == finalLevel) {
                break;
            }
        }
        return steps;
    }

    private static String describe(WorkflowSettings s) {
        return "manager-only up to INR " + s.getManagerOnlyLimit().toPlainString()
                + ", director above INR " + s.getDirectorThreshold().toPlainString();
    }
}
