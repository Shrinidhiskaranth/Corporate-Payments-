package com.nmamit.payments.repository;

import com.nmamit.payments.model.entity.WorkflowSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowSettingsRepository extends JpaRepository<WorkflowSettings, Long> {
}
