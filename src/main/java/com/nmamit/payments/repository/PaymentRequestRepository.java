package com.nmamit.payments.repository;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.User;
import com.nmamit.payments.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long>,
                                                  JpaSpecificationExecutor<PaymentRequest> {

    List<PaymentRequest> findByRequestedByOrderByCreatedAtDesc(User user);

    List<PaymentRequest> findByStatusAndRequestedBy_Department_Id(RequestStatus status, Long departmentId);

    long countByStatus(RequestStatus status);

    /** Loads one request together with the employee and department (needed by JSF pages). */
    @Query("select r from PaymentRequest r join fetch r.requestedBy u left join fetch u.department where r.id = :id")
    Optional<PaymentRequest> findByIdWithDetails(@Param("id") Long id);

    /** Requests in one status, with requester and department loaded, oldest first. */
    @Query("select r from PaymentRequest r join fetch r.requestedBy u left join fetch u.department " +
           "where r.status = :status order by r.createdAt asc")
    List<PaymentRequest> findByStatusWithDetails(@Param("status") RequestStatus status);

    /** Requests in one status from one department, with requester loaded, oldest first. */
    @Query("select r from PaymentRequest r join fetch r.requestedBy u join fetch u.department d " +
           "where r.status = :status and d.id = :departmentId order by r.createdAt asc")
    List<PaymentRequest> findByStatusAndDepartmentWithDetails(@Param("status") RequestStatus status,
                                                              @Param("departmentId") Long departmentId);

    /** Total committed spend of a department (used for the budget check). */
    @Query("select coalesce(sum(r.amount), 0) from PaymentRequest r " +
           "where r.requestedBy.department.id = :departmentId and r.status in :statuses")
    BigDecimal sumAmountByDepartmentAndStatuses(@Param("departmentId") Long departmentId,
                                                @Param("statuses") Collection<RequestStatus> statuses);
}
