package com.nmamit.payments.bean;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.Decision;
import com.nmamit.payments.model.enums.RequestStatus;
import com.nmamit.payments.service.ApprovalService;
import com.nmamit.payments.service.CurrentUserService;
import com.nmamit.payments.service.WorkflowException;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Backing bean for manager/approvals.xhtml, finance/approvals.xhtml and director/approvals.xhtml.
 * Each table row has its own remarks box, stored in the remarks map by request id.
 */
@Component("approvalBean")
@RequestScope
public class ApprovalBean {

    private final CurrentUserService currentUserService;
    private final ApprovalService approvalService;

    @Getter private final Map<Long, String> remarks = new HashMap<>();
    private User user;
    private List<PaymentRequest> managerQueue;
    private List<PaymentRequest> financeQueue;
    private List<PaymentRequest> paymentQueue;
    private List<PaymentRequest> directorQueue;

    public ApprovalBean(CurrentUserService currentUserService, ApprovalService approvalService) {
        this.currentUserService = currentUserService;
        this.approvalService = approvalService;
    }

    private User user() {
        if (user == null) {
            user = currentUserService.get();
        }
        return user;
    }

    public List<PaymentRequest> getManagerQueue() {
        if (managerQueue == null) {
            managerQueue = approvalService.pendingForManager(user());
        }
        return managerQueue;
    }

    public List<PaymentRequest> getFinanceQueue() {
        if (financeQueue == null) {
            financeQueue = approvalService.pendingForFinance(user());
        }
        return financeQueue;
    }

    public List<PaymentRequest> getPaymentQueue() {
        if (paymentQueue == null) {
            paymentQueue = approvalService.awaitingPayment(user());
        }
        return paymentQueue;
    }

    public List<PaymentRequest> getDirectorQueue() {
        if (directorQueue == null) {
            directorQueue = approvalService.pendingForDirector(user());
        }
        return directorQueue;
    }

    public String managerApprove(Long id) {
        return decide(() -> approvalService.managerDecision(id, user(), Decision.APPROVED, remarks.get(id)),
                Decision.APPROVED, "/manager/approvals.xhtml");
    }

    public String managerReject(Long id) {
        return decide(() -> approvalService.managerDecision(id, user(), Decision.REJECTED, remarks.get(id)),
                Decision.REJECTED, "/manager/approvals.xhtml");
    }

    public String financeApprove(Long id) {
        return decide(() -> approvalService.financeDecision(id, user(), Decision.APPROVED, remarks.get(id)),
                Decision.APPROVED, "/finance/approvals.xhtml");
    }

    public String financeReject(Long id) {
        return decide(() -> approvalService.financeDecision(id, user(), Decision.REJECTED, remarks.get(id)),
                Decision.REJECTED, "/finance/approvals.xhtml");
    }

    public String directorApprove(Long id) {
        return decide(() -> approvalService.directorDecision(id, user(), Decision.APPROVED, remarks.get(id)),
                Decision.APPROVED, "/director/approvals.xhtml");
    }

    public String directorReject(Long id) {
        return decide(() -> approvalService.directorDecision(id, user(), Decision.REJECTED, remarks.get(id)),
                Decision.REJECTED, "/director/approvals.xhtml");
    }

    private String decide(Supplier<PaymentRequest> action, Decision decision, String page) {
        try {
            PaymentRequest r = action.get();
            FacesUtil.infoAfterRedirect(r.getRequestNumber() + " " + outcome(r, decision));
            return page + "?faces-redirect=true";
        } catch (WorkflowException e) {
            FacesUtil.error(e.getMessage());
            managerQueue = financeQueue = directorQueue = null; // reload fresh lists
            return null;
        }
    }

    private static String outcome(PaymentRequest r, Decision decision) {
        if (decision == Decision.REJECTED) {
            return "rejected.";
        }
        RequestStatus s = r.getStatus();
        return switch (s) {
            case PENDING_FINANCE -> "approved and sent to finance.";
            case PENDING_DIRECTOR -> "approved and sent to the director (amount above the director threshold).";
            case APPROVED -> "fully approved. It is now ready for payment.";
            default -> "approved.";
        };
    }
}
