package com.nmamit.payments.bean;

import com.nmamit.payments.model.entity.Approval;
import com.nmamit.payments.model.entity.AuditLog;
import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.PaymentTransaction;
import com.nmamit.payments.model.enums.ApprovalLevel;
import com.nmamit.payments.model.enums.Decision;
import com.nmamit.payments.model.enums.RequestStatus;
import com.nmamit.payments.repository.PaymentTransactionRepository;
import com.nmamit.payments.service.ApprovalService;
import com.nmamit.payments.service.AuditService;
import com.nmamit.payments.service.CurrentUserService;
import com.nmamit.payments.service.PaymentRequestService;
import com.nmamit.payments.service.WorkflowException;
import com.nmamit.payments.service.WorkflowSettingsService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.ArrayList;
import java.util.List;

/**
 * Backing bean for request-detail.xhtml?id=..: details, approval route progress, approval history,
 * payment and audit trail.
 */
@Component("requestDetailBean")
@RequestScope
public class RequestDetailBean {

    private final CurrentUserService currentUserService;
    private final PaymentRequestService requestService;
    private final ApprovalService approvalService;
    private final AuditService auditService;
    private final PaymentTransactionRepository transactionRepository;

    @Getter @Setter private Long id;
    @Getter private PaymentRequest request;
    @Getter private List<Approval> approvals = List.of();
    @Getter private List<AuditLog> history = List.of();
    @Getter private PaymentTransaction transaction;

    /** One box in the route progress bar. state: done, rejected, current, upcoming, skipped. */
    @Getter @AllArgsConstructor
    public static class Step {
        private final String name;
        private final String state;
        private final String note;
    }

    public RequestDetailBean(CurrentUserService currentUserService, PaymentRequestService requestService,
                             ApprovalService approvalService, AuditService auditService,
                             PaymentTransactionRepository transactionRepository) {
        this.currentUserService = currentUserService;
        this.requestService = requestService;
        this.approvalService = approvalService;
        this.auditService = auditService;
        this.transactionRepository = transactionRepository;
    }

    public String load() {
        if (id == null) {
            FacesUtil.errorAfterRedirect("No request selected");
            return "/dashboard.xhtml?faces-redirect=true";
        }
        try {
            var viewer = currentUserService.get();
            request = requestService.getForViewing(id, viewer);
            approvals = approvalService.history(id);
            history = auditService.historyOfRequest(id);
            transaction = transactionRepository.findByRequestIdWithDetails(id).orElse(null);
            return null;
        } catch (WorkflowException e) {
            FacesUtil.errorAfterRedirect(e.getMessage());
            return "/dashboard.xhtml?faces-redirect=true";
        }
    }

    /** Route of the request with the state of each step, worked out from its approval history. */
    public List<Step> getSteps() {
        if (request == null) {
            return List.of();
        }
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Submitted", request.getStatus() == RequestStatus.DRAFT ? "upcoming" : "done", null));
        boolean stopped = false;
        for (ApprovalLevel level : WorkflowSettingsService.route(request.getEffectiveFinalLevel())) {
            Approval a = approvals.stream().filter(x -> x.getLevel() == level)
                    .reduce((first, second) -> second).orElse(null);
            String name = FormatBean.levelName(level);
            if (stopped) {
                steps.add(new Step(name, "skipped", null));
            } else if (a != null && a.getDecision() == Decision.REJECTED) {
                steps.add(new Step(name, "rejected", a.getApprover().getFullName()));
                stopped = true;
            } else if (a != null) {
                steps.add(new Step(name, "done", a.getApprover().getFullName()));
            } else if (isWaitingAt(level)) {
                steps.add(new Step(name, "current", null));
            } else if (request.getStatus() == RequestStatus.CANCELLED) {
                steps.add(new Step(name, "skipped", null));
                stopped = true;
            } else {
                steps.add(new Step(name, "upcoming", null));
            }
        }
        String payState = request.getStatus() == RequestStatus.PAID ? "done"
                : request.getStatus() == RequestStatus.APPROVED ? "current"
                : stopped ? "skipped" : "upcoming";
        steps.add(new Step("Paid", payState, null));
        return steps;
    }

    private boolean isWaitingAt(ApprovalLevel level) {
        return switch (level) {
            case MANAGER -> request.getStatus() == RequestStatus.PENDING_MANAGER;
            case FINANCE -> request.getStatus() == RequestStatus.PENDING_FINANCE;
            case DIRECTOR -> request.getStatus() == RequestStatus.PENDING_DIRECTOR;
        };
    }
}
