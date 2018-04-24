package uk.gov.pay.adminusers.app.config;

import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class NotifyConfiguration extends Configuration {

    @Valid
    @NotNull
    private String cardApiKey;

    @Valid
    @NotNull
    private String directDebitApiKey;

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

    // direct-debit specific templates
    @Valid
    @NotNull
    private String mandateFailedTemplateId;

    @Valid
    @NotNull
    private String mandateCancelledTemplateId;

    @Valid
    @NotNull
    private String paymentConfirmedTemplateId;

    public String getMandateFailedTemplateId() {
        return mandateFailedTemplateId;
    }

    public String getMandateCancelledTemplateId() {
        return mandateCancelledTemplateId;
    }

    public String getPaymentConfirmedTemplateId() {
        return paymentConfirmedTemplateId;
    }

    public String getCardApiKey() {
        return cardApiKey;
    }

    public String getDirectDebitApiKey() {
        return directDebitApiKey;
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
