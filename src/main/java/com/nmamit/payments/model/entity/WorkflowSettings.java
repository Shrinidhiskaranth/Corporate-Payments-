package com.nmamit.payments.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Approval rules, editable by the admin. There is exactly one row (id = 1).
 * <ul>
 *   <li>amount &lt;= managerOnlyLimit: manager approval only</li>
 *   <li>amount &lt;= directorThreshold: manager, then finance</li>
 *   <li>above directorThreshold: manager, then finance, then director</li>
 * </ul>
 */
@Entity
@Table(name = "workflow_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class WorkflowSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "manager_only_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal managerOnlyLimit = new BigDecimal("10000.00");

    @Column(name = "director_threshold", nullable = false, precision = 15, scale = 2)
    private BigDecimal directorThreshold = new BigDecimal("100000.00");

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
