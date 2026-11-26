package com.nmamit.payments.repository;

import com.nmamit.payments.model.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /** Same as findByEmail but also loads the department, for use in JSF pages. */
    @EntityGraph(attributePaths = "department")
    Optional<User> findWithDepartmentByEmail(String email);
}
