package com.nmamit.payments.bean;

import com.nmamit.payments.model.entity.Department;
import com.nmamit.payments.repository.DepartmentRepository;
import com.nmamit.payments.repository.PaymentRequestRepository;
import com.nmamit.payments.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;

/** JSF backing bean for index.xhtml (Spring-managed, reachable from EL as #{homeBean}). */
@Component("homeBean")
@RequestScope
public class HomeBean {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PaymentRequestRepository paymentRequestRepository;

    public HomeBean(DepartmentRepository departmentRepository,
                    UserRepository userRepository,
                    PaymentRequestRepository paymentRequestRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.paymentRequestRepository = paymentRequestRepository;
    }

    public List<Department> getDepartments() {
        return departmentRepository.findAll();
    }

    public long getUserCount() {
        return userRepository.count();
    }

    public long getRequestCount() {
        return paymentRequestRepository.count();
    }
}
