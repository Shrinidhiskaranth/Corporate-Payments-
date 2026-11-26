package com.nmamit.payments;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.PaymentTransaction;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.Decision;
import com.nmamit.payments.model.enums.RequestStatus;
import com.nmamit.payments.repository.PaymentTransactionRepository;
import com.nmamit.payments.repository.UserRepository;
import com.nmamit.payments.service.ApprovalService;
import com.nmamit.payments.service.NotAllowedException;
import com.nmamit.payments.service.PaymentRequestService;
import com.nmamit.payments.service.PaymentService;
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
class PaymentServiceTests {

    @Autowired PaymentService paymentService;
    @Autowired ApprovalService approvalService;
    @Autowired PaymentRequestService requestService;
    @Autowired PaymentTransactionRepository transactionRepository;
    @Autowired UserRepository userRepository;

    User employee, manager, finance;

    @BeforeEach
    void setUp() {
        employee = userRepository.findWithDepartmentByEmail("employee@corp.com").orElseThrow();
        manager = userRepository.findWithDepartmentByEmail("manager@corp.com").orElseThrow();
        finance = userRepository.findWithDepartmentByEmail("mvinayakrkamath@gmail.com").orElseThrow();
    }

    private PaymentRequest approved() {
        PaymentRequest r = TestData.submitted(requestService, employee, "12000");
        approvalService.managerDecision(r.getId(), manager, Decision.APPROVED, null);
        approvalService.financeDecision(r.getId(), finance, Decision.APPROVED, null);
        return r;
    }

    @Test
    void payWithCorrectOtp() {
        PaymentRequest r = approved();
        PaymentService.OtpDispatch d = paymentService.startPayment(r.getId(), finance);
        assertThat(d.getCode()).matches("\\d{6}");
        assertThat(d.getSentTo()).as("masked mobile").endsWith(finance.getMobileNumber().substring(6));

        PaymentTransaction tx = paymentService.completePayment(r.getId(), finance, d.getCode());
        assertThat(tx.getReferenceNumber()).startsWith("TXN");
        assertThat(tx.isOtpVerified()).isTrue();
        assertThat(r.getStatus()).isEqualTo(RequestStatus.PAID);
        assertThat(transactionRepository.existsByRequest_Id(r.getId())).isTrue();
    }

    @Test
    void wrongOtpIsRejected() {
        PaymentRequest r = approved();
        PaymentService.OtpDispatch d = paymentService.startPayment(r.getId(), finance);
        String wrong = d.getCode().equals("000000") ? "111111" : "000000";
        assertThatThrownBy(() -> paymentService.completePayment(r.getId(), finance, wrong))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("Wrong OTP");
        assertThat(r.getStatus()).isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    void otpLockedAfterThreeWrongAttempts() {
        PaymentRequest r = approved();
        PaymentService.OtpDispatch d = paymentService.startPayment(r.getId(), finance);
        String wrong = d.getCode().equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> paymentService.completePayment(r.getId(), finance, wrong))
                    .isInstanceOf(WorkflowException.class);
        }
        // even the correct code no longer works; a new OTP is needed
        assertThatThrownBy(() -> paymentService.completePayment(r.getId(), finance, d.getCode()))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("No OTP");
    }

    @Test
    void cannotPayWithoutSendingOtp() {
        PaymentRequest r = approved();
        assertThatThrownBy(() -> paymentService.completePayment(r.getId(), finance, "123456"))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("No OTP");
    }

    @Test
    void cannotPayRequestThatIsNotApproved() {
        PaymentRequest r = TestData.submitted(requestService, employee, "12000");
        assertThatThrownBy(() -> paymentService.startPayment(r.getId(), finance))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("only APPROVED");
    }

    @Test
    void onlyFinanceCanPay() {
        PaymentRequest r = approved();
        assertThatThrownBy(() -> paymentService.startPayment(r.getId(), manager))
                .isInstanceOf(NotAllowedException.class);
    }
}
