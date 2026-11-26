package com.nmamit.payments.service;

import com.nmamit.payments.model.entity.Approval;
import com.nmamit.payments.model.entity.AuditLog;
import com.nmamit.payments.model.entity.Department;
import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.ApprovalLevel;
import com.nmamit.payments.model.enums.AuditAction;
import com.nmamit.payments.model.enums.Decision;
import com.nmamit.payments.model.enums.RequestStatus;
import com.nmamit.payments.model.enums.Role;
import com.nmamit.payments.repository.ApprovalRepository;
import com.nmamit.payments.repository.PaymentRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Amount-based approval workflow. The route is fixed when the request is submitted
 * (see {@link WorkflowSettingsService#requiredLevel}):
 * <pre>
 *   small:  PENDING_MANAGER -> APPROVED
 *   medium: PENDING_MANAGER -> PENDING_FINANCE -> APPROVED
 *   large:  PENDING_MANAGER -> PENDING_FINANCE -> PENDING_DIRECTOR -> APPROVED
 * </pre>
 * Any approver can reject (reason required) which ends the workflow.
 * The department budget is checked at the finance step and at the final approval.
 */
@Service
public class ApprovalService {

    private final PaymentRequestRepository requestRepository;
    private final ApprovalRepository approvalRepository;
    private final AuditService auditService;

    public ApprovalService(PaymentRequestRepository requestRepository,
                           ApprovalRepository approvalRepository,
                           AuditService auditService) {
        this.requestRepository = requestRepository;
        this.approvalRepository = approvalRepository;
        this.auditService = auditService;
    }

    // ---------------------------------------------------------------- queues

    /** Requests waiting for this manager: PENDING_MANAGER from the manager's own department. */
    @Transactional(readOnly = true)
    public List<PaymentRequest> pendingForManager(User manager) {
        requireRole(manager, Role.MANAGER);
        Long deptId = departmentIdOf(manager);
        return requestRepository.findByStatusAndDepartmentWithDetails(RequestStatus.PENDING_MANAGER, deptId);
    }

    /** Requests approved by managers, waiting for finance. */
    @Transactional(readOnly = true)
    public List<PaymentRequest> pendingForFinance(User finance) {
        requireRole(finance, Role.FINANCE);
        return requestRepository.findByStatusWithDetails(RequestStatus.PENDING_FINANCE);
    }

    /** Large requests approved by finance, waiting for a director. */
    @Transactional(readOnly = true)
    public List<PaymentRequest> pendingForDirector(User director) {
        requireRole(director, Role.DIRECTOR);
        return requestRepository.findByStatusWithDetails(RequestStatus.PENDING_DIRECTOR);
    }

    /** Fully approved requests that can now be paid. */
    @Transactional(readOnly = true)
    public List<PaymentRequest> awaitingPayment(User finance) {
        requireRole(finance, Role.FINANCE);
        return requestRepository.findByStatusWithDetails(RequestStatus.APPROVED);
    }

    // ---------------------------------------------------------------- decisions

    /** Manager step: only a manager of the requester's department may decide. */
    @Transactional
    public PaymentRequest managerDecision(Long requestId, User user, Decision decision, String remarks) {
        requireRole(user, Role.MANAGER);
        PaymentRequest request = load(requestId);
        requireStatus(request, RequestStatus.PENDING_MANAGER, "manager approval");
        Department requestDept = request.getRequestedBy().getDepartment();
        if (requestDept == null || !Objects.equals(requestDept.getId(), departmentIdOf(user))) {
            throw new NotAllowedException("You can only approve requests from your own department");
        }
        return decide(request, user, ApprovalLevel.MANAGER, decision, remarks);
    }

    /** Finance step. */
    @Transactional
    public PaymentRequest financeDecision(Long requestId, User user, Decision decision, String remarks) {
        requireRole(user, Role.FINANCE);
        PaymentRequest request = load(requestId);
        requireStatus(request, RequestStatus.PENDING_FINANCE, "finance approval");
        return decide(request, user, ApprovalLevel.FINANCE, decision, remarks);
    }

    /** Director step for large amounts. */
    @Transactional
    public PaymentRequest directorDecision(Long requestId, User director, Decision decision, String remarks) {
        requireRole(director, Role.DIRECTOR);
        PaymentRequest request = load(requestId);
        requireStatus(request, RequestStatus.PENDING_DIRECTOR, "director approval");
        return decide(request, director, ApprovalLevel.DIRECTOR, decision, remarks);
    }

    @Transactional(readOnly = true)
    public List<Approval> history(Long requestId) {
        return approvalRepository.findHistory(requestId);
    }

    /** Status that follows an approval at {@code level} for a request whose route ends at {@code finalLevel}. */
    static RequestStatus nextAfterApproval(ApprovalLevel level, ApprovalLevel finalLevel) {
        if (level.ordinal() >= finalLevel.ordinal()) {
            return RequestStatus.APPROVED;
        }
        return switch (level) {
            case MANAGER -> RequestStatus.PENDING_FINANCE;
            case FINANCE -> RequestStatus.PENDING_DIRECTOR;
            case DIRECTOR -> RequestStatus.APPROVED;
        };
    }

    // ---------------------------------------------------------------- internals

    private PaymentRequest decide(PaymentRequest request, User approver, ApprovalLevel level,
                                  Decision decision, String remarks) {
        checkRemarks(decision, remarks);
        RequestStatus next;
        if (decision == Decision.APPROVED) {
            next = nextAfterApproval(level, request.getEffectiveFinalLevel());
            if (level == ApprovalLevel.FINANCE || next == RequestStatus.APPROVED) {
                checkBudget(request);
            }
        } else {
            next = RequestStatus.REJECTED;
        }

        record(request, approver, level, decision, remarks);
        request.setStatus(next);
        audit(request, approver, level, decision, remarks, next);
        return request;
    }

    /** Approved + paid spend of the department plus this request must stay within its budget limit. */
    private void checkBudget(PaymentRequest request) {
        Department dept = request.getRequestedBy().getDepartment();
        if (dept == null || dept.getBudgetLimit() == null) {
            return; // no limit set
        }
        BigDecimal committed = requestRepository.sumAmountByDepartmentAndStatuses(
                dept.getId(), EnumSet.of(RequestStatus.APPROVED, RequestStatus.PAID));
        BigDecimal after = committed.add(request.getAmount());
        if (after.compareTo(dept.getBudgetLimit()) > 0) {
            throw new WorkflowException("Budget exceeded for " + dept.getName() + ": limit INR "
                    + dept.getBudgetLimit() + ", already committed INR " + committed
                    + ", this request INR " + request.getAmount());
        }
    }

    private void record(PaymentRequest request, User approver, ApprovalLevel level, Decision decision,
                        String remarks) {
        Approval approval = new Approval();
        approval.setRequest(request);
        approval.setApprover(approver);
        approval.setLevel(level);
        approval.setDecision(decision);
        approval.setRemarks(remarks == null || remarks.isBlank() ? null : remarks.trim());
        approvalRepository.save(approval);
    }

    private void audit(PaymentRequest request, User user, ApprovalLevel level, Decision decision,
                       String remarks, RequestStatus next) {
        AuditAction action = decision == Decision.APPROVED ? AuditAction.APPROVE : AuditAction.REJECT;
        String who = capitalize(level.name());
        String text = who + " " + (decision == Decision.APPROVED ? "approved " : "rejected ")
                + request.getRequestNumber()
                + (decision == Decision.APPROVED ? " -> " + next : "")
                + (remarks == null || remarks.isBlank() ? "" : ": " + remarks.trim());
        auditService.log(user, action, AuditLog.TYPE_REQUEST, request.getId(), text);
    }

    private static void checkRemarks(Decision decision, String remarks) {
        if (decision == null) {
            throw new WorkflowException("Choose approve or reject");
        }
        if (decision == Decision.REJECTED && (remarks == null || remarks.trim().length() < 5)) {
            throw new WorkflowException("Please give a reason (at least 5 characters) when rejecting");
        }
        if (remarks != null && remarks.length() > 300) {
            throw new WorkflowException("Remarks can be at most 300 characters");
        }
    }

    private PaymentRequest load(Long requestId) {
        if (requestId == null) {
            throw new WorkflowException("No request selected");
        }
        return requestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new WorkflowException("Request " + requestId + " not found"));
    }

    private static void requireStatus(PaymentRequest request, RequestStatus expected, String what) {
        if (request.getStatus() != expected) {
            throw new WorkflowException("This request is not waiting for " + what);
        }
    }

    private static void requireRole(User user, Role... roles) {
        for (Role r : roles) {
            if (user.getRole() == r) {
                return;
            }
        }
        throw new NotAllowedException("This action needs the " + roles[0] + " role");
    }

    private static Long departmentIdOf(User manager) {
        if (manager.getDepartment() == null) {
            throw new WorkflowException("Your account is not linked to a department");
        }
        return manager.getDepartment().getId();
    }

    private static String capitalize(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
