package com.nmamit.payments.bean;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.PaymentTransaction;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.service.CurrentUserService;
import com.nmamit.payments.service.PaymentService;
import com.nmamit.payments.service.WorkflowException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/** Backing bean for finance/pay.xhtml?id=..: send OTP, then confirm payment with it. */
@Component("paymentBean")
@RequestScope
public class PaymentBean {

    private final CurrentUserService currentUserService;
    private final PaymentService paymentService;
    private final boolean showOtpOnScreen;

    @Getter @Setter private Long id;
    @Getter @Setter private String otp;
    private PaymentRequest request;
    private User user;

    public PaymentBean(CurrentUserService currentUserService, PaymentService paymentService,
                       @Value("${app.otp.show-on-screen:true}") boolean showOtpOnScreen) {
        this.currentUserService = currentUserService;
        this.paymentService = paymentService;
        this.showOtpOnScreen = showOtpOnScreen;
    }

    private User user() {
        if (user == null) {
            user = currentUserService.get();
        }
        return user;
    }

    /** Called on the first GET; sends the user back to the finance page if the request cannot be paid. */
    public String load() {
        if (id == null) {
            FacesUtil.errorAfterRedirect("No request selected");
            return "/finance/approvals.xhtml?faces-redirect=true";
        }
        try {
            getRequest();
            return null;
        } catch (WorkflowException e) {
            FacesUtil.errorAfterRedirect(e.getMessage());
            return "/finance/approvals.xhtml?faces-redirect=true";
        }
    }

    /**
     * The request being paid. On a postback (Send OTP / Confirm) JSF evaluates the page's rendered checks
     * before the hidden id field is applied, so with no id yet this returns null instead of failing.
     */
    public PaymentRequest getRequest() {
        if (id == null) {
            return null;
        }
        if (request == null) {
            request = paymentService.getPayable(id, user());
        }
        return request;
    }

    public boolean isOtpSent() {
        return id != null && paymentService.hasActiveOtp(id, user());
    }

    public String sendOtp() {
        try {
            PaymentService.OtpDispatch d = paymentService.startPayment(id, user());
            String msg = "OTP sent to your registered mobile " + d.getSentTo() + " (simulated SMS). "
                    + "It is valid for 5 minutes.";
            if (showOtpOnScreen) {
                msg += " Demo mode - your OTP is " + d.getCode() + ".";
            }
            FacesUtil.info(msg);
        } catch (WorkflowException e) {
            FacesUtil.error(e.getMessage());
        }
        return null;
    }

    public String confirm() {
        try {
            PaymentTransaction tx = paymentService.completePayment(id, user(), otp);
            FacesUtil.infoAfterRedirect("Payment successful. Reference number " + tx.getReferenceNumber()
                    + ". Use \"Download receipt (PDF)\" in the Payment advice section below to save the receipt.");
            return "/request-detail.xhtml?faces-redirect=true&id=" + id;
        } catch (WorkflowException e) {
            FacesUtil.error(e.getMessage());
            otp = null;
            return null;
        }
    }
}
