package com.nmamit.payments.controller;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.PaymentTransaction;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.AuditAction;
import com.nmamit.payments.model.entity.AuditLog;
import com.nmamit.payments.repository.ApprovalRepository;
import com.nmamit.payments.repository.PaymentTransactionRepository;
import com.nmamit.payments.service.AuditService;
import com.nmamit.payments.service.CurrentUserService;
import com.nmamit.payments.service.PaymentRequestService;
import com.nmamit.payments.service.ReceiptService;
import com.nmamit.payments.service.WorkflowException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /receipts/{requestId}.pdf - the PDF receipt of a paid request.
 * Same access rules as the request page: finance, director and admin see all; the owner and the
 * department manager their department's.
 */
@RestController
public class ReceiptController {

    private final CurrentUserService currentUserService;
    private final PaymentRequestService requestService;
    private final PaymentTransactionRepository transactionRepository;
    private final ApprovalRepository approvalRepository;
    private final ReceiptService receiptService;
    private final AuditService auditService;

    public ReceiptController(CurrentUserService currentUserService, PaymentRequestService requestService,
                             PaymentTransactionRepository transactionRepository, ApprovalRepository approvalRepository,
                             ReceiptService receiptService, AuditService auditService) {
        this.currentUserService = currentUserService;
        this.requestService = requestService;
        this.transactionRepository = transactionRepository;
        this.approvalRepository = approvalRepository;
        this.receiptService = receiptService;
        this.auditService = auditService;
    }

    @GetMapping("/receipts/{requestId}.pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> receipt(@PathVariable Long requestId) {
        User viewer = currentUserService.get();
        PaymentRequest request = requestService.getForViewing(requestId, viewer); // access check
        PaymentTransaction tx = transactionRepository.findByRequestIdWithDetails(requestId)
                .orElseThrow(() -> new WorkflowException(request.getRequestNumber() + " has not been paid yet"));
        byte[] pdf = receiptService.generate(tx, request, approvalRepository.findHistory(requestId));
        String file = ReceiptService.receiptNumber(tx) + ".pdf";
        auditService.logSeparately(viewer, AuditAction.EXPORT, AuditLog.TYPE_REQUEST, requestId,
                "Downloaded receipt " + file + " for " + request.getRequestNumber());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(file).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
