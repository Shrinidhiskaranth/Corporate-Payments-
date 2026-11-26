package com.nmamit.payments;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.ApprovalLevel;
import com.nmamit.payments.model.enums.AuditAction;
import com.nmamit.payments.model.enums.PaymentCategory;
import com.nmamit.payments.model.enums.RequestStatus;
import com.nmamit.payments.repository.AuditLogRepository;
import com.nmamit.payments.repository.UserRepository;
import com.nmamit.payments.service.NotAllowedException;
import com.nmamit.payments.service.PaymentRequestService;
import com.nmamit.payments.service.WorkflowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PaymentRequestServiceTests {

    @Autowired PaymentRequestService service;
    @Autowired UserRepository userRepository;
    @Autowired AuditLogRepository auditLogRepository;

    User employee, manager;

    @BeforeEach
    void setUp() {
        employee = userRepository.findWithDepartmentByEmail("employee@corp.com").orElseThrow();
        manager = userRepository.findWithDepartmentByEmail("manager@corp.com").orElseThrow();
    }

    private PaymentRequest draft(String amount) {
        return TestData.draft(service, employee, amount);
    }

    @Test
    void createDraftAssignsNumberAndStatus() {
        long before = auditLogRepository.countByAction(AuditAction.CREATE_REQUEST);
        PaymentRequest r = draft("45000.00");
        assertThat(r.getId()).isNotNull();
        assertThat(r.getRequestNumber()).matches("PR-\\d{4}-\\d{4}");
        assertThat(r.getStatus()).isEqualTo(RequestStatus.DRAFT);
        assertThat(auditLogRepository.countByAction(AuditAction.CREATE_REQUEST)).isEqualTo(before + 1);
    }

    @Test
    void draftCanBeEdited() {
        PaymentRequest r = draft("45000.00");
        service.updateDraft(r.getId(), employee, new PaymentRequestService.RequestForm("New title", null,
                new BigDecimal("47000"), PaymentCategory.TRAINING, "Other Supplier", "987654321098", null));
        assertThat(r.getAmount()).isEqualByComparingTo("47000");
        assertThat(r.getCategory()).isEqualTo(PaymentCategory.TRAINING);
    }

    @Test
    void submitMovesDraftToPendingManagerAndFixesRoute() {
        PaymentRequest small = draft("5000");
        service.submit(small.getId(), employee);
        assertThat(small.getStatus()).isEqualTo(RequestStatus.PENDING_MANAGER);
        assertThat(small.getFinalLevel()).isEqualTo(ApprovalLevel.MANAGER);

        PaymentRequest large = draft("250000");
        service.submit(large.getId(), employee);
        assertThat(large.getFinalLevel()).isEqualTo(ApprovalLevel.DIRECTOR);
    }

    @Test
    void cannotSubmitTwice() {
        PaymentRequest r = draft("45000.00");
        service.submit(r.getId(), employee);
        assertThatThrownBy(() -> service.submit(r.getId(), employee))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("Only draft");
    }

    @Test
    void cancelAllowedWhilePendingManager() {
        PaymentRequest r = draft("45000.00");
        service.submit(r.getId(), employee);
        service.cancel(r.getId(), employee);
        assertThat(r.getStatus()).isEqualTo(RequestStatus.CANCELLED);
    }

    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> draft("0"))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void rejectsInvalidAccountNumber() {
        assertThatThrownBy(() -> service.createDraft(employee, new PaymentRequestService.RequestForm("Laptop", null,
                new BigDecimal("5000"), PaymentCategory.OTHER, "ABC Computers", "12AB", null)))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("account number");
    }

    @Test
    void pastDueDateIsRefused() {
        assertThatThrownBy(() -> service.createDraft(employee, new PaymentRequestService.RequestForm("Laptop", null,
                new BigDecimal("5000"), PaymentCategory.OTHER, "ABC Computers", "123456789012",
                LocalDate.now().minusDays(1))))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("past");
    }

    @Test
    void onlyEmployeesCanRaiseRequests() {
        assertThatThrownBy(() -> service.createDraft(manager, TestData.form("5000")))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("Only employees");
    }

    @Test
    void otherUsersCannotSubmitSomeoneElsesRequest() {
        PaymentRequest r = draft("45000.00");
        assertThatThrownBy(() -> service.submit(r.getId(), manager))
                .isInstanceOf(NotAllowedException.class)
                .hasMessageContaining("your own");
    }
}
