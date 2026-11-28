package com.nmamit.payments.bean;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.service.CurrentUserService;
import com.nmamit.payments.service.PaymentRequestService;
import com.nmamit.payments.service.WorkflowException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/** Backing bean for request-detail.xhtml?id=..: shows one payment request. */
@Component("requestDetailBean")
@RequestScope
public class RequestDetailBean {

    private final CurrentUserService currentUserService;
    private final PaymentRequestService requestService;

    @Getter @Setter private Long id;
    @Getter private PaymentRequest request;

    public RequestDetailBean(CurrentUserService currentUserService, PaymentRequestService requestService) {
        this.currentUserService = currentUserService;
        this.requestService = requestService;
    }

    public String load() {
        if (id == null) {
            FacesUtil.errorAfterRedirect("No request selected");
            return "/dashboard.xhtml?faces-redirect=true";
        }
        try {
            request = requestService.getForViewing(id, currentUserService.get());
            return null;
        } catch (WorkflowException e) {
            FacesUtil.errorAfterRedirect(e.getMessage());
            return "/dashboard.xhtml?faces-redirect=true";
        }
    }
}
