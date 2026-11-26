package com.nmamit.payments.service;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.PaymentCategory;
import com.nmamit.payments.model.enums.RequestStatus;
import com.nmamit.payments.model.enums.Role;
import com.nmamit.payments.repository.PaymentRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Payment requests raised by employees: create as a draft, edit while it is a draft, submit for approval
 * or cancel.
 */
@Service
public class PaymentRequestService {

    static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000.00"); // 1 crore

    private final PaymentRequestRepository requestRepository;

    public PaymentRequestService(PaymentRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    /** What is typed into the request form. */
    public record RequestForm(String title, String description, BigDecimal amount, PaymentCategory category,
                              String beneficiaryName, String beneficiaryAccount, LocalDate dueDate) {
    }

    /** An employee creates a request; it starts as a DRAFT. */
    @Transactional
    public PaymentRequest createDraft(User employee, RequestForm f) {
        if (employee.getRole() != Role.EMPLOYEE) {
            throw new NotAllowedException("Only employees can raise payment requests");
        }
        PaymentRequest request = new PaymentRequest();
        request.setRequestNumber(nextRequestNumber());
        request.setStatus(RequestStatus.DRAFT);
        request.setRequestedBy(employee);
        apply(request, f);
        return requestRepository.save(request);
    }

    /** Changes a request that is still a DRAFT. */
    @Transactional
    public PaymentRequest updateDraft(Long requestId, User employee, RequestForm f) {
        PaymentRequest request = findOwned(requestId, employee);
        if (request.getStatus() != RequestStatus.DRAFT) {
            throw new WorkflowException("Only draft requests can be edited");
        }
        apply(request, f);
        return request;
    }

    /** DRAFT -> PENDING_MANAGER. */
    @Transactional
    public PaymentRequest submit(Long requestId, User employee) {
        PaymentRequest request = findOwned(requestId, employee);
        if (request.getStatus() != RequestStatus.DRAFT) {
            throw new WorkflowException("Only draft requests can be submitted");
        }
        request.setStatus(RequestStatus.PENDING_MANAGER);
        return request;
    }

    /** DRAFT or PENDING_MANAGER -> CANCELLED. Not allowed once a manager has approved it. */
    @Transactional
    public PaymentRequest cancel(Long requestId, User employee) {
        PaymentRequest request = findOwned(requestId, employee);
        if (!EnumSet.of(RequestStatus.DRAFT, RequestStatus.PENDING_MANAGER).contains(request.getStatus())) {
            throw new WorkflowException("This request can no longer be cancelled");
        }
        request.setStatus(RequestStatus.CANCELLED);
        return request;
    }

    @Transactional(readOnly = true)
    public List<PaymentRequest> myRequests(User employee) {
        return requestRepository.findByRequestedByOrderByCreatedAtDesc(employee);
    }

    /** A draft the employee owns, for the edit form. */
    @Transactional(readOnly = true)
    public PaymentRequest getOwnDraft(Long requestId, User employee) {
        PaymentRequest request = findOwned(requestId, employee);
        if (request.getStatus() != RequestStatus.DRAFT) {
            throw new WorkflowException("Only draft requests can be edited");
        }
        return request;
    }

    /**
     * Loads a request for the detail page. Employees see their own; managers their department's;
     * finance and admin everything.
     */
    @Transactional(readOnly = true)
    public PaymentRequest getForViewing(Long requestId, User viewer) {
        if (requestId == null) {
            throw new WorkflowException("No request selected");
        }
        PaymentRequest request = requestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new WorkflowException("Request " + requestId + " not found"));
        User owner = request.getRequestedBy();
        boolean allowed = switch (viewer.getRole()) {
            case ADMIN, FINANCE -> true;
            case MANAGER -> owner.getDepartment() != null && viewer.getDepartment() != null
                    && Objects.equals(owner.getDepartment().getId(), viewer.getDepartment().getId());
            case EMPLOYEE -> Objects.equals(owner.getId(), viewer.getId());
        };
        if (!allowed) {
            throw new NotAllowedException("You do not have permission to view this request");
        }
        return request;
    }

    // ------------------------------------------------------------------ internals

    private static void apply(PaymentRequest request, RequestForm f) {
        validate(f);
        request.setTitle(f.title().trim());
        request.setDescription(f.description() == null || f.description().isBlank() ? null : f.description().trim());
        request.setAmount(f.amount());
        request.setCategory(f.category());
        request.setBeneficiaryName(f.beneficiaryName().trim());
        request.setBeneficiaryAccount(f.beneficiaryAccount().trim());
        request.setDueDate(f.dueDate());
    }

    private PaymentRequest findOwned(Long requestId, User employee) {
        if (requestId == null) {
            throw new WorkflowException("No request selected");
        }
        PaymentRequest request = requestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new WorkflowException("Request " + requestId + " not found"));
        if (!request.getRequestedBy().getId().equals(employee.getId())) {
            throw new NotAllowedException("You can only change your own requests");
        }
        return request;
    }

    private static void validate(RequestForm f) {
        if (f.title() == null || f.title().trim().length() < 3) {
            throw new WorkflowException("Title must have at least 3 characters");
        }
        if (f.amount() == null || f.amount().signum() <= 0) {
            throw new WorkflowException("Amount must be greater than zero");
        }
        if (f.amount().scale() > 2) {
            throw new WorkflowException("Amount can have at most 2 decimal places");
        }
        if (f.amount().compareTo(MAX_AMOUNT) > 0) {
            throw new WorkflowException("Amount cannot exceed 1,00,00,000.00");
        }
        if (f.category() == null) {
            throw new WorkflowException("Choose a category");
        }
        if (f.beneficiaryName() == null || f.beneficiaryName().trim().length() < 3) {
            throw new WorkflowException("Beneficiary name must have at least 3 characters");
        }
        if (f.beneficiaryAccount() == null || !f.beneficiaryAccount().trim().matches("\\d{9,18}")) {
            throw new WorkflowException("Beneficiary account number must be 9 to 18 digits");
        }
        if (f.dueDate() != null && f.dueDate().isBefore(LocalDate.now())) {
            throw new WorkflowException("Due date cannot be in the past");
        }
    }

    /** Builds numbers like PR-2026-0001. Requests are never deleted, so the count only grows. */
    private String nextRequestNumber() {
        long next = requestRepository.count() + 1;
        return String.format("PR-%d-%04d", Year.now().getValue(), next);
    }
}
