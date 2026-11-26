package com.nmamit.payments.model.enums;

/** Approval steps in the order they happen. A request needs the steps up to its final level. */
public enum ApprovalLevel {
    MANAGER, FINANCE, DIRECTOR
}
