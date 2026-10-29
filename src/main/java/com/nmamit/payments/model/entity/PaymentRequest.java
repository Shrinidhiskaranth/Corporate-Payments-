package com.nmamit.payments.model.entity;

import com.nmamit.payments.model.enums.PaymentCategory;
import com.nmamit.payments.model.enums.RequestStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_requests")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class PaymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_number", nullable = false, unique = true, length = 20)
    private String requestNumber;     // e.g. PR-2026-0001

    @NotBlank
    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    @DecimalMin(value = "0.01")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentCategory category;

    @Column(name = "beneficiary_name", nullable = false, length = 100)
    private String beneficiaryName;

    @Column(name = "beneficiary_account", nullable = false, length = 30)
    private String beneficiaryAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.DRAFT;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
