package com.nmamit.payments.service;

import com.nmamit.payments.model.entity.AuditLog;
import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.PaymentTransaction;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.AuditAction;
import com.nmamit.payments.model.enums.RequestStatus;
import com.nmamit.payments.model.enums.Role;
import com.nmamit.payments.repository.PaymentRequestRepository;
import com.nmamit.payments.repository.PaymentTransactionRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Pays an APPROVED request in two steps:
 *  1. startPayment  - sends an OTP to the finance officer's registered mobile number (simulated SMS:
 *                     the code is written to the application log)
 *  2. completePayment - checks the OTP, records a transaction and marks the request PAID
 * Money is not actually transferred; the transaction is a record with a reference number.
 */
@Service
public class PaymentService {

    private static final DateTimeFormatter REF_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRequestRepository requestRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final OtpService otpService;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();

    public PaymentService(PaymentRequestRepository requestRepository,
                          PaymentTransactionRepository transactionRepository,
                          OtpService otpService, AuditService auditService) {
        this.requestRepository = requestRepository;
        this.transactionRepository = transactionRepository;
        this.otpService = otpService;
        this.auditService = auditService;
    }

    /**
     * Result of sending an OTP: the masked mobile number it went to, and the code itself
     * (returned only so the page can show it in demo mode, as no SMS gateway is connected).
     */
    @Getter
    @AllArgsConstructor
    public static class OtpDispatch {
        private final String sentTo;
        private final String code;
    }

    @Transactional(readOnly = true)
    public PaymentRequest getPayable(Long requestId, User finance) {
        requireFinance(finance);
        PaymentRequest request = load(requestId);
        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new WorkflowException(request.getRequestNumber() + " is " + request.getStatus()
                    + "; only APPROVED requests can be paid");
        }
        return request;
    }

    @Transactional
    public OtpDispatch startPayment(Long requestId, User finance) {
        PaymentRequest request = getPayable(requestId, finance);
        String code = otpService.generate(request.getId(), finance.getId());
        String masked = OtpService.mask(finance.getMobileNumber());
        log.info("[SIMULATED SMS] to {}: Your OTP for paying {} (INR {}) is {}. Valid for 5 minutes.",
                masked, request.getRequestNumber(), request.getAmount().toPlainString(), code);
        auditService.log(finance, AuditAction.OTP_SENT, AuditLog.TYPE_REQUEST, request.getId(),
                "OTP sent to " + masked + " for " + request.getRequestNumber());
        return new OtpDispatch(masked, code);
    }

    public boolean hasActiveOtp(Long requestId, User finance) {
        return otpService.hasActiveCode(requestId, finance.getId());
    }

    /**
     * Checks the OTP and records the payment. The PDF receipt is then available to download
     * from the request page (/receipts/{requestId}.pdf, see ReceiptController).
     */
    @Transactional
    public PaymentTransaction completePayment(Long requestId, User finance, String otp) {
        PaymentRequest request = getPayable(requestId, finance);
        if (transactionRepository.existsByRequest_Id(request.getId())) {
            throw new WorkflowException(request.getRequestNumber() + " has already been paid");
        }
        try {
            otpService.verify(request.getId(), finance.getId(), otp);
        } catch (WorkflowException ex) {
            // kept even though this transaction rolls back
            auditService.logSeparately(finance, AuditAction.OTP_FAILED, AuditLog.TYPE_REQUEST,
                    request.getId(), "OTP check failed for " + request.getRequestNumber() + ": " + ex.getMessage());
            throw ex;
        }

        PaymentTransaction tx = new PaymentTransaction();
        tx.setRequest(request);
        tx.setAmount(request.getAmount());
        tx.setProcessedBy(finance);
        tx.setOtpVerified(true);
        tx.setReferenceNumber(newReference());
        tx.setPaidToAccount(request.getBeneficiaryAccount());
        transactionRepository.save(tx);

        request.setStatus(RequestStatus.PAID);
        auditService.log(finance, AuditAction.PAY, AuditLog.TYPE_REQUEST, request.getId(),
                "Paid " + request.getRequestNumber() + " INR " + request.getAmount()
                        + " to " + request.getBeneficiaryName() + ", ref " + tx.getReferenceNumber());
        return tx;
    }

    /** e.g. TXN20261111094512-4821 */
    private String newReference() {
        String ref;
        do {
            ref = "TXN" + LocalDateTime.now().format(REF_TIME) + "-" + String.format("%04d", random.nextInt(10_000));
        } while (transactionRepository.existsByReferenceNumber(ref));
        return ref;
    }

    private PaymentRequest load(Long requestId) {
        if (requestId == null) {
            throw new WorkflowException("No request selected");
        }
        return requestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new WorkflowException("Request " + requestId + " not found"));
    }

    private static void requireFinance(User user) {
        if (user.getRole() != Role.FINANCE) {
            throw new NotAllowedException("Only finance officers can make payments");
        }
    }
}
