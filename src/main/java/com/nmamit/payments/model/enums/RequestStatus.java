package com.nmamit.payments.model.enums;

public enum RequestStatus {
    DRAFT, PENDING_MANAGER, APPROVED, REJECTED, CANCELLED;

    /** True while the request is waiting for an approver (someone has to decide). */
    public boolean isPending() {
        return this == PENDING_MANAGER;
    }
}
