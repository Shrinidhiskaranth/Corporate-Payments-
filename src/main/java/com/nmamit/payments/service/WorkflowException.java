package com.nmamit.payments.service;

/** Thrown when an action breaks a business rule, e.g. submitting a request that is not a draft. */
public class WorkflowException extends RuntimeException {
    public WorkflowException(String message) {
        super(message);
    }
}
