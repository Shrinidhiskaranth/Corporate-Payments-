package com.nmamit.payments.repository;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {
    List<PaymentRequest> findByRequestedByOrderByCreatedAtDesc(User user);
    List<PaymentRequest> findByStatus(RequestStatus status);
    List<PaymentRequest> findByStatusAndRequestedBy_Department_Id(RequestStatus status, Long departmentId);
    long countByStatus(RequestStatus status);
}
