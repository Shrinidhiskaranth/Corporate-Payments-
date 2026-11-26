package com.nmamit.payments.bean;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.RequestStatus;
import com.nmamit.payments.model.enums.Role;
import com.nmamit.payments.repository.PaymentRequestRepository;
import com.nmamit.payments.service.ApprovalService;
import com.nmamit.payments.service.CurrentUserService;
import com.nmamit.payments.service.PaymentRequestService;
import com.nmamit.payments.service.WorkflowException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;

/** Backing bean for dashboard.xhtml: a summary that depends on the user's role. */
@Component("dashboardBean")
@RequestScope
public class DashboardBean {

    private final CurrentUserService currentUserService;
    private final PaymentRequestService requestService;
    private final ApprovalService approvalService;
    private final PaymentRequestRepository requestRepository;

    private User user;
    private List<PaymentRequest> myRequests;
    private List<PaymentRequest> managerQueue;
    private List<PaymentRequest> financeQueue;

    public DashboardBean(CurrentUserService currentUserService, PaymentRequestService requestService,
                         ApprovalService approvalService, PaymentRequestRepository requestRepository) {
        this.currentUserService = currentUserService;
        this.requestService = requestService;
        this.approvalService = approvalService;
        this.requestRepository = requestRepository;
    }

    public User getUser() {
        if (user == null) {
            user = currentUserService.get();
        }
        return user;
    }

    public String getDepartmentName() {
        return getUser().getDepartment() == null ? "-" : getUser().getDepartment().getName();
    }

    public boolean isEmployee() { return getUser().getRole() == Role.EMPLOYEE; }
    public boolean isManager()  { return getUser().getRole() == Role.MANAGER; }
    public boolean isFinance()  { return getUser().getRole() == Role.FINANCE; }
    public boolean isDirector() { return getUser().getRole() == Role.DIRECTOR; }
    public boolean isAdmin()    { return getUser().getRole() == Role.ADMIN; }

    public List<PaymentRequest> getMyRequests() {
        if (myRequests == null) {
            myRequests = isEmployee() ? requestService.myRequests(getUser()) : List.of();
        }
        return myRequests;
    }

    public long countMine(String status) {
        RequestStatus s = RequestStatus.valueOf(status);
        return getMyRequests().stream().filter(r -> r.getStatus() == s).count();
    }

    public int getManagerPendingCount() {
        return isManager() ? managerQueue().size() : 0;
    }

    public int getFinancePendingCount() {
        return isFinance() ? financeQueue().size() : 0;
    }

    public int getDirectorPendingCount() {
        return isDirector() ? approvalService.pendingForDirector(getUser()).size() : 0;
    }

    private List<PaymentRequest> managerQueue() {
        if (managerQueue == null) {
            managerQueue = approvalService.pendingForManager(getUser());
        }
        return managerQueue;
    }

    private List<PaymentRequest> financeQueue() {
        if (financeQueue == null) {
            financeQueue = approvalService.pendingForFinance(getUser());
        }
        return financeQueue;
    }

    public int getAwaitingPaymentCount() {
        return isFinance() ? approvalService.awaitingPayment(getUser()).size() : 0;
    }

    public long getTotalRequests() {
        return requestRepository.count();
    }

    public long getTotalPaid() {
        return requestRepository.countByStatus(RequestStatus.PAID);
    }

    public boolean canEdit(PaymentRequest r)   { return r.getStatus() == RequestStatus.DRAFT; }
    public boolean canSubmit(PaymentRequest r) { return r.getStatus() == RequestStatus.DRAFT; }
    public boolean canCancel(PaymentRequest r) {
        return r.getStatus() == RequestStatus.DRAFT || r.getStatus() == RequestStatus.PENDING_MANAGER;
    }

    public String submit(Long id) {
        try {
            PaymentRequest r = requestService.submit(id, getUser());
            FacesUtil.infoAfterRedirect(r.getRequestNumber() + " submitted for manager approval. Approval route: "
                    + new FormatBean().route(r) + ".");
        } catch (WorkflowException e) {
            FacesUtil.errorAfterRedirect(e.getMessage());
        }
        return "/dashboard.xhtml?faces-redirect=true";
    }

    public String cancel(Long id) {
        try {
            PaymentRequest r = requestService.cancel(id, getUser());
            FacesUtil.infoAfterRedirect(r.getRequestNumber() + " cancelled.");
        } catch (WorkflowException e) {
            FacesUtil.errorAfterRedirect(e.getMessage());
        }
        return "/dashboard.xhtml?faces-redirect=true";
    }
}
