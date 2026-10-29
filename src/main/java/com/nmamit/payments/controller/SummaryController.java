package com.nmamit.payments.controller;

import com.nmamit.payments.model.enums.RequestStatus;
import com.nmamit.payments.repository.DepartmentRepository;
import com.nmamit.payments.repository.PaymentRequestRepository;
import com.nmamit.payments.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** REST endpoint that confirms the database connection and shows record counts. */
@RestController
@RequestMapping("/api")
public class SummaryController {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PaymentRequestRepository paymentRequestRepository;

    public SummaryController(DepartmentRepository departmentRepository,
                             UserRepository userRepository,
                             PaymentRequestRepository paymentRequestRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.paymentRequestRepository = paymentRequestRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("departments", departmentRepository.count());
        result.put("users", userRepository.count());
        result.put("paymentRequests", paymentRequestRepository.count());
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (RequestStatus s : RequestStatus.values()) {
            byStatus.put(s.name(), paymentRequestRepository.countByStatus(s));
        }
        result.put("requestsByStatus", byStatus);
        return result;
    }
}
