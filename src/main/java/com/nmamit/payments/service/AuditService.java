package com.nmamit.payments.service;

import com.nmamit.payments.model.entity.AuditLog;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.AuditAction;
import com.nmamit.payments.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Central place for writing and reading audit records. */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Writes an audit row as part of the caller's transaction (rolled back together with it). */
    @Transactional
    public void log(User user, AuditAction action, String entityType, Long entityId, String details) {
        auditLogRepository.save(build(user, action, entityType, entityId, details));
    }

    /**
     * Writes an audit row in its own transaction, so it is kept even if the caller's
     * transaction is rolled back. Used for failed OTP attempts.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSeparately(User user, AuditAction action, String entityType, Long entityId, String details) {
        auditLogRepository.save(build(user, action, entityType, entityId, details));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> latest() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDescIdDesc();
    }

    /**
     * Audit rows matching the filters, newest first, at most {@code limit}.
     * Text matches the details or the user's email; dates are inclusive.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> search(AuditAction action, String text, LocalDate from, LocalDate to, int limit) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            From<?, User> user;
            boolean countQuery = query.getResultType() == Long.class || query.getResultType() == long.class;
            if (countQuery) {
                user = root.join("user", JoinType.LEFT);
            } else {
                @SuppressWarnings("unchecked")
                From<?, User> fetched = (From<?, User>) root.<AuditLog, User>fetch("user", JoinType.LEFT);
                user = fetched;
            }
            List<Predicate> p = new ArrayList<>();
            if (action != null) {
                p.add(cb.equal(root.get("action"), action));
            }
            if (text != null && !text.isBlank()) {
                String like = "%" + text.trim().toLowerCase().replace("\\", "\\\\")
                        .replace("%", "\\%").replace("_", "\\_") + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("details")), like, '\\'),
                            cb.like(cb.lower(user.get("email")), like, '\\')));
            }
            if (from != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                p.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        return auditLogRepository.findAll(spec, PageRequest.of(0, limit, sort)).getContent();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> historyOfRequest(Long requestId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc(AuditLog.TYPE_REQUEST, requestId);
    }

    private AuditLog build(User user, AuditAction action, String entityType, Long entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setUser(user);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details == null ? null : truncate(details, 500));
        return entry;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
