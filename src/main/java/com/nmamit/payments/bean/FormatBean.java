package com.nmamit.payments.bean;

import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.enums.ApprovalLevel;
import com.nmamit.payments.service.WorkflowSettingsService;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Display helpers used from any page as #{fmt...}: the approval route of a request.
 * Stateless, so one shared instance is enough.
 */
@Component("fmt")
public class FormatBean {

    /** e.g. "Manager → Finance → Director". Drafts show the route the current rules would give. */
    public String route(PaymentRequest r) {
        ApprovalLevel finalLevel = r.getEffectiveFinalLevel();
        return WorkflowSettingsService.route(finalLevel).stream()
                .map(FormatBean::levelName)
                .collect(Collectors.joining(" → "));
    }

    public static String levelName(ApprovalLevel level) {
        return level.name().charAt(0) + level.name().substring(1).toLowerCase();
    }
}
