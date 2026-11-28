package com.nmamit.payments.bean;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.PaymentCategory;
import com.nmamit.payments.service.CurrentUserService;
import com.nmamit.payments.service.PaymentRequestService;
import com.nmamit.payments.service.WorkflowException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Backing bean for employee/new-request.xhtml: create a request, or edit a draft (?id=..). */
@Component("newRequestBean")
@RequestScope
public class NewRequestBean {

    private final CurrentUserService currentUserService;
    private final PaymentRequestService requestService;

    @Getter @Setter private Long id;
    @Getter @Setter private String title;
    @Getter @Setter private String description;
    @Getter @Setter private BigDecimal amount;
    @Getter @Setter private PaymentCategory category = PaymentCategory.OTHER;
    @Getter @Setter private String beneficiaryName;
    @Getter @Setter private String beneficiaryAccount;
    @Getter @Setter private LocalDate dueDate;
    @Getter @Setter private boolean submitNow = true;
    @Getter private String requestNumber;

    private User user;

    public NewRequestBean(CurrentUserService currentUserService, PaymentRequestService requestService) {
        this.currentUserService = currentUserService;
        this.requestService = requestService;
    }

    private User user() {
        if (user == null) {
            user = currentUserService.get();
        }
        return user;
    }

    public PaymentCategory[] getCategories() {
        return PaymentCategory.values();
    }

    public boolean isEditing() {
        return id != null;
    }

    /** f:viewAction on the first GET: fills the form when an employee edits a draft. */
    public String load() {
        if (id == null) {
            return null;
        }
        try {
            PaymentRequest r = requestService.getOwnDraft(id, user());
            requestNumber = r.getRequestNumber();
            title = r.getTitle();
            description = r.getDescription();
            amount = r.getAmount();
            category = r.getCategory();
            beneficiaryName = r.getBeneficiaryName();
            beneficiaryAccount = r.getBeneficiaryAccount();
            dueDate = r.getDueDate();
            submitNow = false;
            return null;
        } catch (WorkflowException e) {
            FacesUtil.errorAfterRedirect(e.getMessage());
            return "/dashboard.xhtml?faces-redirect=true";
        }
    }

    private PaymentRequestService.RequestForm form() {
        return new PaymentRequestService.RequestForm(title, description, amount, category,
                beneficiaryName, beneficiaryAccount, dueDate);
    }

    /** Saves (and optionally submits) the request. */
    public String save() {
        try {
            PaymentRequest r = id == null ? requestService.createDraft(user(), form())
                                          : requestService.updateDraft(id, user(), form());
            String msg = r.getRequestNumber() + (id == null ? " saved as draft." : " updated.");
            if (submitNow) {
                requestService.submit(r.getId(), user());
                msg = r.getRequestNumber() + " submitted for manager approval.";
            }
            FacesUtil.infoAfterRedirect(msg);
            return "/dashboard.xhtml?faces-redirect=true";
        } catch (WorkflowException e) {
            FacesUtil.error(e.getMessage());
            return null; // stay on the form so the user can fix it
        }
    }
}
