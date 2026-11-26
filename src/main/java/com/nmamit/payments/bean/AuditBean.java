package com.nmamit.payments.bean;

import com.nmamit.payments.model.entity.AuditLog;
import com.nmamit.payments.model.enums.AuditAction;
import com.nmamit.payments.service.AuditService;
import com.nmamit.payments.service.WorkflowException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Backing bean for admin/audit.xhtml: filter by action, text and date range (GET parameters). */
@Component("auditBean")
@RequestScope
public class AuditBean {

    public static final int PAGE_LIMIT = 200;

    private final HttpServletRequest http;
    private final AuditService auditService;

    private List<AuditLog> rows;
    private String error;

    public AuditBean(HttpServletRequest http, AuditService auditService) {
        this.http = http;
        this.auditService = auditService;
    }

    public String param(String name) {
        String v = http.getParameter(name);
        return v == null ? "" : v;
    }

    public List<AuditLog> getRows() {
        if (rows == null) {
            try {
                Criteria c = Criteria.parse(http);
                rows = auditService.search(c.action, c.text, c.from, c.to, PAGE_LIMIT);
            } catch (WorkflowException e) {
                error = e.getMessage();
                rows = List.of();
            }
        }
        return rows;
    }

    public String getError() {
        getRows();
        return error;
    }

    public AuditAction[] getActions() {
        return AuditAction.values();
    }

    /** Parsed audit filter. */
    public static final class Criteria {
        public AuditAction action;
        public String text;
        public LocalDate from;
        public LocalDate to;

        public static Criteria parse(HttpServletRequest req) {
            Criteria c = new Criteria();
            String a = blankToNull(req.getParameter("action"));
            if (a != null) {
                try {
                    c.action = AuditAction.valueOf(a);
                } catch (IllegalArgumentException e) {
                    throw new WorkflowException("Invalid action: " + a);
                }
            }
            c.text = blankToNull(req.getParameter("q"));
            if (c.text != null && c.text.length() > 100) {
                throw new WorkflowException("Search text can be at most 100 characters");
            }
            c.from = date(req.getParameter("from"), "From date");
            c.to = date(req.getParameter("to"), "To date");
            if (c.from != null && c.to != null && c.from.isAfter(c.to)) {
                throw new WorkflowException("From date must be on or before the To date");
            }
            return c;
        }

        private static LocalDate date(String raw, String what) {
            String v = blankToNull(raw);
            if (v == null) {
                return null;
            }
            try {
                return LocalDate.parse(v);
            } catch (DateTimeParseException e) {
                throw new WorkflowException(what + " must be a date like 2026-11-17");
            }
        }

        private static String blankToNull(String s) {
            return s == null || s.isBlank() ? null : s.trim();
        }
    }
}
