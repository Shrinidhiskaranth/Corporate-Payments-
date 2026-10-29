package com.nmamit.payments;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.PaymentCategory;
import com.nmamit.payments.model.enums.RequestStatus;
import com.nmamit.payments.repository.DepartmentRepository;
import com.nmamit.payments.repository.PaymentRequestRepository;
import com.nmamit.payments.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CorporatePaymentsApplicationTests {

    @Autowired DepartmentRepository departmentRepository;
    @Autowired UserRepository userRepository;
    @Autowired PaymentRequestRepository paymentRequestRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void seedDataIsLoaded() {
        assertThat(departmentRepository.count()).isEqualTo(3);
        assertThat(userRepository.count()).isEqualTo(4);
        User admin = userRepository.findByEmail("admin@corp.com").orElseThrow();
        assertThat(passwordEncoder.matches("password123", admin.getPassword())).isTrue();
    }

    @Test
    void paymentRequestCanBeSavedAndQueried() {
        User employee = userRepository.findByEmail("employee@corp.com").orElseThrow();

        PaymentRequest pr = new PaymentRequest();
        pr.setRequestNumber("PR-2026-0001");
        pr.setTitle("Laptop purchase");
        pr.setAmount(new BigDecimal("65000.00"));
        pr.setCategory(PaymentCategory.PURCHASE);
        pr.setBeneficiaryName("ABC Computers");
        pr.setBeneficiaryAccount("123456789012");
        pr.setRequestedBy(employee);
        paymentRequestRepository.save(pr);

        assertThat(pr.getCreatedAt()).isNotNull();
        assertThat(paymentRequestRepository.countByStatus(RequestStatus.DRAFT)).isEqualTo(1);
        assertThat(paymentRequestRepository.findByRequestedByOrderByCreatedAtDesc(employee)).hasSize(1);
    }
}
