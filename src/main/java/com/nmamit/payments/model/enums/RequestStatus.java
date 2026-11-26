package com.nmamit.payments.model.enums;

public enum RequestStatus {
    DRAFT, PENDING_MANAGER, PENDING_FINANCE, PENDING_DIRECTOR, APPROVED, REJECTED, PAID, CANCELLED;

    /** True while the request is waiting for an approver (someone has to decide). */
    public boolean isPending() {
        return this == PENDING_MANAGER || this == PENDING_FINANCE || this == PENDING_DIRECTOR;
    }
}
