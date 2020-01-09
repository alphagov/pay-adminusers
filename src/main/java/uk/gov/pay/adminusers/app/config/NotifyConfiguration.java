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
    private String signInOtpSmsTemplateId;

    @Valid
    @NotNull
    private String changeSignIn2faToSmsOtpSmsTemplateId;

    @Valid
    @NotNull
    private String selfInitiatedCreateUserAndServiceOtpSmsTemplateId;

    @Valid
    @NotNull
    private String createUserInResponseToInvitationToServiceOtpSmsTemplateId;

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
    
    @Valid
    @NotNull
    private String liveAccountCreatedEmailTemplateId;

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

    public String getSignInOtpSmsTemplateId() {
        return signInOtpSmsTemplateId;
    }

    public String getChangeSignIn2faToSmsOtpSmsTemplateId() {
        return changeSignIn2faToSmsOtpSmsTemplateId;
    }

    public String getSelfInitiatedCreateUserAndServiceOtpSmsTemplateId() {
        return selfInitiatedCreateUserAndServiceOtpSmsTemplateId;
    }

    public String getCreateUserInResponseToInvitationToServiceOtpSmsTemplateId() {
        return createUserInResponseToInvitationToServiceOtpSmsTemplateId;
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

    public String getLiveAccountCreatedEmailTemplateId() {
        return liveAccountCreatedEmailTemplateId;
    }
}
