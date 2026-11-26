package com.nmamit.payments;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.PaymentCategory;
import com.nmamit.payments.service.PaymentRequestService;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Helpers shared by the service tests. */
final class TestData {

    private TestData() {
    }

    static PaymentRequestService.RequestForm form(String amount) {
        return new PaymentRequestService.RequestForm("Test purchase", "for testing", new BigDecimal(amount),
                PaymentCategory.OTHER, "Test Supplier", "123456789012", LocalDate.now().plusDays(10));
    }

    static PaymentRequest draft(PaymentRequestService service, User employee, String amount) {
        return service.createDraft(employee, form(amount));
    }

    /** Creates and submits a request, so it is PENDING_MANAGER. */
    static PaymentRequest submitted(PaymentRequestService service, User employee, String amount) {
        PaymentRequest r = draft(service, employee, amount);
        service.submit(r.getId(), employee);
        return r;
    }
}
