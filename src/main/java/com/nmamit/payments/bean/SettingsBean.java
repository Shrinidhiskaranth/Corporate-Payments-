package com.nmamit.payments.bean;

import com.nmamit.payments.model.entity.WorkflowSettings;
import com.nmamit.payments.service.CurrentUserService;
import com.nmamit.payments.service.WorkflowException;
import com.nmamit.payments.service.WorkflowSettingsService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.math.BigDecimal;

/** Backing bean for admin/settings.xhtml: the amount thresholds of the approval route. */
@Component("settingsBean")
@RequestScope
public class SettingsBean {

    private final CurrentUserService currentUserService;
    private final WorkflowSettingsService settingsService;

    @Getter @Setter private BigDecimal managerOnlyLimit;
    @Getter @Setter private BigDecimal directorThreshold;
    @Getter private WorkflowSettings current;

    public SettingsBean(CurrentUserService currentUserService, WorkflowSettingsService settingsService) {
        this.currentUserService = currentUserService;
        this.settingsService = settingsService;
    }

    /** f:viewAction on GET: fill the form with the stored values. */
    public String load() {
        current = settingsService.get();
        managerOnlyLimit = current.getManagerOnlyLimit();
        directorThreshold = current.getDirectorThreshold();
        return null;
    }

    public WorkflowSettings getCurrent() {
        if (current == null) {
            current = settingsService.get();
        }
        return current;
    }

    public String save() {
        try {
            settingsService.update(currentUserService.get(), managerOnlyLimit, directorThreshold);
            FacesUtil.infoAfterRedirect("Workflow settings saved. New rules apply to requests submitted from now on.");
            return "/admin/settings.xhtml?faces-redirect=true";
        } catch (WorkflowException e) {
            FacesUtil.error(e.getMessage());
            return null;
        }
    }
}
