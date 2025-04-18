package uk.gov.pay.adminusers.app.config;

import io.dropwizard.core.Configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class NotifyConfiguration extends Configuration {

    @Valid
    @NotNull
    private String cardApiKey;

    @Valid
    @NotNull
    private String notificationBaseURL;

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

    @Valid
    @NotNull
    private String stripeDisputeCreatedEmailTemplateId;

    @Valid
    @NotNull
    private String stripeDisputeLostEmailTemplateId;

    @Valid
    @NotNull
    private String stripeDisputeLostAndServiceChargedEmailTemplateId;

    @Valid
    @NotNull
    private String stripeDisputeEvidenceSubmittedEmailTemplateId;

    @Valid
    @NotNull
    private String stripeDisputeWonEmailTemplateId;

    @Valid
    @NotNull
    private String notifyEmailReplyToSupportId;

    public String getCardApiKey() {
        return cardApiKey;
    }

    public String getNotificationBaseURL() {
        return notificationBaseURL;
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

    public String getStripeDisputeCreatedEmailTemplateId() {
        return stripeDisputeCreatedEmailTemplateId;
    }

    public String getStripeDisputeLostEmailTemplateId() {
        return stripeDisputeLostEmailTemplateId;
    }

    public String getStripeDisputeLostAndServiceChargedEmailTemplateId() {
        return stripeDisputeLostAndServiceChargedEmailTemplateId;
    }

    public String getStripeDisputeEvidenceSubmittedEmailTemplateId() {
        return stripeDisputeEvidenceSubmittedEmailTemplateId;
    }

    public String getStripeDisputeWonEmailTemplateId() {
        return stripeDisputeWonEmailTemplateId;
    }

    public String getNotifyEmailReplyToSupportId() {
        return notifyEmailReplyToSupportId;
    }
}
