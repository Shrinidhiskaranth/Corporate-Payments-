package com.nmamit.payments.service;

/** Thrown when the user's role or ownership does not allow an action (shown as HTTP 403 in the REST API). */
public class NotAllowedException extends WorkflowException {
    public NotAllowedException(String message) {
        super(message);
    }
}
