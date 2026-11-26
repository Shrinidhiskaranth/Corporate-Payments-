package com.nmamit.payments.service;

import com.nmamit.payments.model.entity.Approval;
import com.nmamit.payments.model.entity.Department;
import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.ApprovalLevel;
import com.nmamit.payments.model.enums.Decision;
import com.nmamit.payments.model.enums.RequestStatus;
import com.nmamit.payments.model.enums.Role;
import com.nmamit.payments.repository.ApprovalRepository;
import com.nmamit.payments.repository.PaymentRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Manager approval:
 * <pre>
 *   PENDING_MANAGER -> APPROVED or REJECTED
 * </pre>
 * Only a manager of the requester's department can decide. A reason is required to reject.
 */
@Service
public class ApprovalService {

    private final PaymentRequestRepository requestRepository;
    private final ApprovalRepository approvalRepository;

    public ApprovalService(PaymentRequestRepository requestRepository, ApprovalRepository approvalRepository) {
        this.requestRepository = requestRepository;
        this.approvalRepository = approvalRepository;
    }

    /** Requests waiting for this manager: PENDING_MANAGER from the manager's own department. */
    @Transactional(readOnly = true)
    public List<PaymentRequest> pendingForManager(User manager) {
        requireManager(manager);
        Long deptId = departmentIdOf(manager);
        return requestRepository.findByStatusAndDepartmentWithDetails(RequestStatus.PENDING_MANAGER, deptId);
    }

    /** Manager step: only a manager of the requester's department may decide. */
    @Transactional
    public PaymentRequest managerDecision(Long requestId, User manager, Decision decision, String remarks) {
        requireManager(manager);
        PaymentRequest request = load(requestId);
        if (request.getStatus() != RequestStatus.PENDING_MANAGER) {
            throw new WorkflowException("This request is not waiting for manager approval");
        }
        Department requestDept = request.getRequestedBy().getDepartment();
        if (requestDept == null || !Objects.equals(requestDept.getId(), departmentIdOf(manager))) {
            throw new NotAllowedException("You can only approve requests from your own department");
        }
        checkRemarks(decision, remarks);

        Approval approval = new Approval();
        approval.setRequest(request);
        approval.setApprover(manager);
        approval.setLevel(ApprovalLevel.MANAGER);
        approval.setDecision(decision);
        approval.setRemarks(remarks == null || remarks.isBlank() ? null : remarks.trim());
        approvalRepository.save(approval);

        request.setStatus(decision == Decision.APPROVED ? RequestStatus.APPROVED : RequestStatus.REJECTED);
        return request;
    }

    @Transactional(readOnly = true)
    public List<Approval> history(Long requestId) {
        return approvalRepository.findHistory(requestId);
    }

    // ---------------------------------------------------------------- internals

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

    private static void requireManager(User user) {
        if (user.getRole() != Role.MANAGER) {
            throw new NotAllowedException("This action needs the MANAGER role");
        }
    }

    private static Long departmentIdOf(User manager) {
        if (manager.getDepartment() == null) {
            throw new WorkflowException("Your account is not linked to a department");
        }
        return manager.getDepartment().getId();
    }
}
