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
    private String inviteEmailTemplateId;

    @Valid
    @NotNull
    private String forgottenPasswordEmailTemplateId;

    public String getApiKey() {
        return apiKey;
    }

    public String getNotificationBaseURL() {
        return notificationBaseURL;
    }

    public String getSecondFactorSmsTemplateId() {
        return secondFactorSmsTemplateId;
    }

    public String getInviteEmailTemplateId() {
        return inviteEmailTemplateId;
    }

    public String getForgottenPasswordEmailTemplateId() {
        return forgottenPasswordEmailTemplateId;
    }
}
