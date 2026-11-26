package com.nmamit.payments.model.entity;

import com.nmamit.payments.model.enums.AuditAction;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

/** Permanent record of who did what. Rows are only ever inserted, never updated. */
@Entity
@Table(name = "audit_logs")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class AuditLog {

    public static final String TYPE_REQUEST = "REQUEST";
    public static final String TYPE_USER = "USER";
    public static final String TYPE_DEPARTMENT = "DEPARTMENT";
    public static final String TYPE_SETTINGS = "SETTINGS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)   // plain VARCHAR, so new enum values never need a schema change
    @Column(nullable = false, length = 50, updatable = false)
    private AuditAction action;

    /** What kind of record entityId points to: REQUEST, USER or DEPARTMENT. */
    @Column(name = "entity_type", length = 20, updatable = false)
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    private Long entityId;

    @Column(length = 500, updatable = false)
    private String details;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
