package com.nmamit.payments.model.entity;

import com.nmamit.payments.model.enums.ApprovalLevel;
import com.nmamit.payments.model.enums.Decision;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

/** One manager, finance or director decision on a payment request. */
@Entity
@Table(name = "approvals")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private PaymentRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_id")
    private User approver;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)   // plain VARCHAR, so new enum values never need a schema change
    @Column(nullable = false, length = 20)
    private ApprovalLevel level;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)   // plain VARCHAR, so new enum values never need a schema change
    @Column(nullable = false, length = 20)
    private Decision decision;

    @Column(length = 300)
    private String remarks;

    @CreatedDate
    @Column(name = "decided_at", nullable = false, updatable = false)
    private LocalDateTime decidedAt;
}
