package com.nmamit.payments;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.ApprovalLevel;
import com.nmamit.payments.model.enums.Decision;
import com.nmamit.payments.model.enums.RequestStatus;
import com.nmamit.payments.repository.UserRepository;
import com.nmamit.payments.service.ApprovalService;
import com.nmamit.payments.service.NotAllowedException;
import com.nmamit.payments.service.PaymentRequestService;
import com.nmamit.payments.service.WorkflowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ApprovalServiceTests {

    @Autowired ApprovalService approvalService;
    @Autowired PaymentRequestService requestService;
    @Autowired UserRepository userRepository;

    User employee, manager, hrManager, finance;

    @BeforeEach
    void setUp() {
        employee = userRepository.findWithDepartmentByEmail("employee@corp.com").orElseThrow();
        manager = userRepository.findWithDepartmentByEmail("manager@corp.com").orElseThrow();
        hrManager = userRepository.findWithDepartmentByEmail("manager.hr@corp.com").orElseThrow();
        finance = userRepository.findWithDepartmentByEmail("mvinayakrkamath@gmail.com").orElseThrow();
    }

    @Test
    void managerSeesOnlyOwnDepartmentQueue() {
        PaymentRequest r = TestData.submitted(requestService, employee, "5000");
        assertThat(approvalService.pendingForManager(manager)).extracting(PaymentRequest::getId).contains(r.getId());
        assertThat(approvalService.pendingForManager(hrManager)).extracting(PaymentRequest::getId).doesNotContain(r.getId());
    }

    @Test
    void managerApprovalMovesToFinance() {
        PaymentRequest r = TestData.submitted(requestService, employee, "50000");
        approvalService.managerDecision(r.getId(), manager, Decision.APPROVED, "ok");
        assertThat(r.getStatus()).isEqualTo(RequestStatus.PENDING_FINANCE);
        assertThat(approvalService.history(r.getId()))
                .singleElement()
                .satisfies(a -> {
                    assertThat(a.getLevel()).isEqualTo(ApprovalLevel.MANAGER);
                    assertThat(a.getDecision()).isEqualTo(Decision.APPROVED);
                });
    }

    @Test
    void rejectWithoutReasonIsRefused() {
        PaymentRequest r = TestData.submitted(requestService, employee, "5000");
        assertThatThrownBy(() -> approvalService.managerDecision(r.getId(), manager, Decision.REJECTED, ""))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void managerRejectionEndsWorkflow() {
        PaymentRequest r = TestData.submitted(requestService, employee, "5000");
        approvalService.managerDecision(r.getId(), manager, Decision.REJECTED, "Not needed this quarter");
        assertThat(r.getStatus()).isEqualTo(RequestStatus.REJECTED);
    }

    @Test
    void otherDepartmentManagerCannotDecide() {
        PaymentRequest r = TestData.submitted(requestService, employee, "5000");
        assertThatThrownBy(() -> approvalService.managerDecision(r.getId(), hrManager, Decision.APPROVED, null))
                .isInstanceOf(NotAllowedException.class);
    }

    @Test
    void financeApprovalAfterManagerApproval() {
        PaymentRequest r = TestData.submitted(requestService, employee, "50000");
        approvalService.managerDecision(r.getId(), manager, Decision.APPROVED, null);
        approvalService.financeDecision(r.getId(), finance, Decision.APPROVED, null);
        assertThat(r.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(approvalService.history(r.getId())).hasSize(2);
    }

    @Test
    void financeCannotSkipManager() {
        PaymentRequest r = TestData.submitted(requestService, employee, "5000");
        assertThatThrownBy(() -> approvalService.financeDecision(r.getId(), finance, Decision.APPROVED, null))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("not waiting for finance");
    }

    @Test
    void financeApprovalBlockedWhenBudgetExceeded() {
        // IT budget limit is 20,00,000
        PaymentRequest r = TestData.submitted(requestService, employee, "2500000");
        approvalService.managerDecision(r.getId(), manager, Decision.APPROVED, null);
        assertThatThrownBy(() -> approvalService.financeDecision(r.getId(), finance, Decision.APPROVED, null))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("Budget exceeded");
    }

    @Test
    void employeeCannotApprove() {
        PaymentRequest r = TestData.submitted(requestService, employee, "5000");
        assertThatThrownBy(() -> approvalService.managerDecision(r.getId(), employee, Decision.APPROVED, null))
                .isInstanceOf(NotAllowedException.class);
    }
}
