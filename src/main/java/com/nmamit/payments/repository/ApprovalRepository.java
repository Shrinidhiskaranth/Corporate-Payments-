package com.nmamit.payments.repository;

import com.nmamit.payments.model.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    @Query("select a from Approval a join fetch a.approver where a.request.id = :requestId " +
           "order by a.decidedAt asc, a.id asc")
    List<Approval> findHistory(@Param("requestId") Long requestId);
}
