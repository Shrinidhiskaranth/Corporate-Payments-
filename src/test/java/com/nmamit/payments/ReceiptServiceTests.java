package com.nmamit.payments;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.PaymentTransaction;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.Decision;
import com.nmamit.payments.repository.ApprovalRepository;
import com.nmamit.payments.repository.UserRepository;
import com.nmamit.payments.service.ApprovalService;
import com.nmamit.payments.service.PaymentRequestService;
import com.nmamit.payments.service.PaymentService;
import com.nmamit.payments.service.ReceiptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReceiptServiceTests {

    @Autowired ReceiptService receiptService;
    @Autowired PaymentService paymentService;
    @Autowired ApprovalService approvalService;
    @Autowired PaymentRequestService requestService;
    @Autowired ApprovalRepository approvalRepository;
    @Autowired UserRepository userRepository;

    @Test
    void receiptPdfIsGeneratedAfterPayment() {
        User employee = userRepository.findWithDepartmentByEmail("employee@corp.com").orElseThrow();
        User manager = userRepository.findWithDepartmentByEmail("manager@corp.com").orElseThrow();
        User finance = userRepository.findWithDepartmentByEmail("mvinayakrkamath@gmail.com").orElseThrow();

        PaymentRequest r = TestData.submitted(requestService, employee, "44840.50");
        approvalService.managerDecision(r.getId(), manager, Decision.APPROVED, null);
        approvalService.financeDecision(r.getId(), finance, Decision.APPROVED, null);
        PaymentService.OtpDispatch d = paymentService.startPayment(r.getId(), finance);
        PaymentTransaction tx = paymentService.completePayment(r.getId(), finance, d.getCode());

        byte[] pdf = receiptService.generate(tx, r, approvalRepository.findHistory(r.getId()));
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdf.length).as("small enough to email").isLessThan(60_000);
        assertThat(ReceiptService.receiptNumber(tx)).matches("RCPT-\\d{4}-\\d{4}");
        try {
            java.nio.file.Files.write(java.nio.file.Path.of("target", "sample-receipt.pdf"), pdf); // for a visual check
        } catch (java.io.IOException ignored) {
            // the sample file is only a convenience
        }
    }
}
