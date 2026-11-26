package com.nmamit.payments.repository;

import com.nmamit.payments.model.entity.AuditLog;
import com.nmamit.payments.model.enums.AuditAction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    @EntityGraph(attributePaths = "user")
    List<AuditLog> findTop100ByOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = "user")
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc(String entityType, Long entityId);

    long countByAction(AuditAction action);
}
