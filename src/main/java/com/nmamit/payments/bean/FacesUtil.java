package com.nmamit.payments.bean;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

/** Small helpers for showing messages on JSF pages. */
public final class FacesUtil {

    private FacesUtil() {
    }

    /** Message shown on the current page. */
    public static void info(String msg) {
        add(FacesMessage.SEVERITY_INFO, msg, false);
    }

    public static void error(String msg) {
        add(FacesMessage.SEVERITY_ERROR, msg, false);
    }

    /** Message kept across a faces-redirect (flash scope). */
    public static void infoAfterRedirect(String msg) {
        add(FacesMessage.SEVERITY_INFO, msg, true);
    }

    public static void errorAfterRedirect(String msg) {
        add(FacesMessage.SEVERITY_ERROR, msg, true);
    }

    private static void add(FacesMessage.Severity severity, String msg, boolean keep) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.addMessage(null, new FacesMessage(severity, msg, null));
        if (keep) {
            ctx.getExternalContext().getFlash().setKeepMessages(true);
        }
    }
}
