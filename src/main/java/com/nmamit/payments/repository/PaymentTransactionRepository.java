package com.nmamit.payments.repository;

import com.nmamit.payments.model.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    boolean existsByRequest_Id(Long requestId);

    boolean existsByReferenceNumber(String referenceNumber);

    @Query("select t from PaymentTransaction t join fetch t.processedBy where t.request.id = :requestId")
    Optional<PaymentTransaction> findByRequestIdWithDetails(@Param("requestId") Long requestId);
}
