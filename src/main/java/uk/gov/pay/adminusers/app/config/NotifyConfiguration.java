package uk.gov.pay.adminusers.app.config;

import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class NotifyConfiguration extends Configuration {

    @Valid
    @NotNull
    private String apiKey;

    @Valid
    @NotNull
    private String notificationBaseURL;

    @Valid
    @NotNull
    private String secondFactorSmsTemplateId;

    @Valid
    @NotNull
    private String inviteUserEmailTemplateId;

    @Valid
    @NotNull
    private String inviteUserExistingEmailTemplateId;

    @Valid
    @NotNull
    private String inviteServiceEmailTemplateId;

    @Valid
    @NotNull
    private String forgottenPasswordEmailTemplateId;

    @Valid
    @NotNull
    private String inviteServiceUserExistsEmailTemplateId;

    @Valid
    @NotNull
    private String inviteServiceUserDisabledEmailTemplateId;

    public String getApiKey() {
        return apiKey;
    }

    public String getNotificationBaseURL() {
        return notificationBaseURL;
    }

    public String getSecondFactorSmsTemplateId() {
        return secondFactorSmsTemplateId;
    }

    public String getInviteUserEmailTemplateId() {
        return inviteUserEmailTemplateId;
    }

    public String getForgottenPasswordEmailTemplateId() {
        return forgottenPasswordEmailTemplateId;
    }

    public String getInviteServiceEmailTemplateId() {
        return inviteServiceEmailTemplateId;
    }

    public String getInviteServiceUserExistsEmailTemplateId() {
        return inviteServiceUserExistsEmailTemplateId;
    }

    public String getInviteServiceUserDisabledEmailTemplateId() {
        return inviteServiceUserDisabledEmailTemplateId;
    }

    public String getInviteUserExistingEmailTemplateId() {
        return inviteUserExistingEmailTemplateId;
    }
}
