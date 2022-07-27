package uk.gov.pay.adminusers.service;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.NotifyConfiguration;
import uk.gov.pay.adminusers.app.config.NotifyDirectDebitConfiguration;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.model.Service.DEFAULT_NAME_VALUE;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.userNotificationError;

public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final NotifyClientProvider notifyClientProvider;
    private final MetricRegistry metricRegistry;
    private final NotifyConfiguration notifyConfiguration;
    private final NotifyDirectDebitConfiguration notifyDirectDebitConfiguration;

    private final String signInOtpSmsTemplateId;
    private final String changeSignIn2faToSmsOtpSmsTemplateId;
    private final String selfInitiatedCreateUserAndServiceOtpSmsTemplateId;
    private final String createUserInResponseToInvitationToServiceOtpSmsTemplateId;

    private final String inviteEmailTemplateId;
    private final String forgottenPasswordEmailTemplateId;
    private final String inviteExistingUserEmailTemplateId;

    private final String stripeDisputeCreatedEmailTemplateId;
    private final String stripeDisputeLostEmailTemplateId;
    private final String stripeDisputeLostAndServiceChargedEmailTemplateId;
    private final String stripeDisputeEvidenceSubmittedEmailTemplateId;
    private final String stripeDisputeWonEmailTemplateId;
    private final String notifyEmailReplyToSupportId;
    private final Instant emailNotificationsForLivePaymentsDisputeUpdatesFrom;
    private final Instant emailNotificationsForTestPaymentsDisputeUpdatesFrom;

    public NotificationService(NotifyClientProvider notifyClientProvider,
                               NotifyConfiguration notifyConfiguration,
                               NotifyDirectDebitConfiguration notifyDirectDebitConfiguration,
                               MetricRegistry metricRegistry) {
        
        this.notifyClientProvider = notifyClientProvider;
        this.notifyConfiguration = notifyConfiguration;
        this.notifyDirectDebitConfiguration = notifyDirectDebitConfiguration;

        this.signInOtpSmsTemplateId = notifyConfiguration.getSignInOtpSmsTemplateId();
        this.changeSignIn2faToSmsOtpSmsTemplateId = notifyConfiguration.getChangeSignIn2faToSmsOtpSmsTemplateId();
        this.selfInitiatedCreateUserAndServiceOtpSmsTemplateId = notifyConfiguration.getSelfInitiatedCreateUserAndServiceOtpSmsTemplateId();
        this.createUserInResponseToInvitationToServiceOtpSmsTemplateId = notifyConfiguration.getCreateUserInResponseToInvitationToServiceOtpSmsTemplateId();

        this.inviteEmailTemplateId = notifyConfiguration.getInviteUserEmailTemplateId();
        this.inviteExistingUserEmailTemplateId = notifyConfiguration.getInviteUserExistingEmailTemplateId();
        this.forgottenPasswordEmailTemplateId = notifyConfiguration.getForgottenPasswordEmailTemplateId();
        
        this.stripeDisputeCreatedEmailTemplateId = notifyConfiguration.getStripeDisputeCreatedEmailTemplateId();
        this.stripeDisputeLostEmailTemplateId = notifyConfiguration.getStripeDisputeLostEmailTemplateId();
        this.stripeDisputeLostAndServiceChargedEmailTemplateId = notifyConfiguration.getStripeDisputeLostAndServiceChargedEmailTemplateId();
        this.stripeDisputeEvidenceSubmittedEmailTemplateId = notifyConfiguration.getStripeDisputeEvidenceSubmittedEmailTemplateId();
        this.stripeDisputeWonEmailTemplateId = notifyConfiguration.getStripeDisputeWonEmailTemplateId();
        this.notifyEmailReplyToSupportId = notifyConfiguration.getNotifyEmailReplyToSupportId();
        this.emailNotificationsForLivePaymentsDisputeUpdatesFrom = notifyConfiguration.getEnableEmailNotificationsForLivePaymentsDisputeUpdatesFrom();
        this.emailNotificationsForTestPaymentsDisputeUpdatesFrom = notifyConfiguration.getEnableEmailNotificationsForTestPaymentsDisputeUpdatesFrom();

        this.metricRegistry = metricRegistry;
    }

    public NotifyDirectDebitConfiguration getNotifyDirectDebitConfiguration() {
        return notifyDirectDebitConfiguration;
    }

    public String sendSecondFactorPasscodeSms(String phoneNumber, String passcode, OtpNotifySmsTemplateId otpNotifySmsTemplateId) {
        Stopwatch responseTimeStopwatch = Stopwatch.createStarted();
        try {
            SendSmsResponse response = notifyClientProvider.get().sendSms(resolveOtpNotifySmsTemplateId(otpNotifySmsTemplateId),
                    TelephoneNumberUtility.formatToE164(phoneNumber), Map.of("code", passcode), null);
            return response.getNotificationId().toString();
        } catch (NotificationClientException e) {
            metricRegistry.counter("notify-operations.sms.failures").inc();
            LOGGER.info("Error sending Sms: " + e.getMessage());
            throw userNotificationError(e);
        } finally {
            responseTimeStopwatch.stop();
            metricRegistry.histogram("notify-operations.sms.response_time").update(responseTimeStopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    public String sendInviteEmail(String sender, String email, String inviteUrl) {
        Map<String, String> personalisation = Map.of(
                "username", sender,
                "link", inviteUrl);
        return sendEmail(inviteEmailTemplateId, email, personalisation);
    }

    public String sendServiceInviteEmail(String email, String inviteUrl) {
        Map<String, String> personalisation = Map.of(
                "name", email,
                "link", inviteUrl);
        return sendEmail(notifyConfiguration.getInviteServiceEmailTemplateId(), email, personalisation);
    }

    public String sendForgottenPasswordEmail(String email, String forgottenPasswordUrl) {
        Map<String, String> personalisation = Map.of("code", forgottenPasswordUrl);
        return sendEmail(forgottenPasswordEmailTemplateId, email, personalisation);
    }

    public String sendServiceInviteUserExistsEmail(String email, String signInLink, String forgottenPasswordLink, String feedbackLink) {
        Map<String, String> personalisation = Map.of(
                "signin_link", signInLink,
                "forgotten_password_link", forgottenPasswordLink,
                "feedback_link", feedbackLink);
        return sendEmail(notifyConfiguration.getInviteServiceUserExistsEmailTemplateId(), email, personalisation);
    }

    public String sendServiceInviteUserDisabledEmail(String email, String supportUrl) {
        Map<String, String> personalisation = Map.of("feedback_link", supportUrl);
        return sendEmail(notifyConfiguration.getInviteServiceUserDisabledEmailTemplateId(), email, personalisation);
    }

    public String sendInviteExistingUserEmail(String sender, String email, String inviteUrl, String serviceName) {
        String collaborateServiceNamePart;
        String joinServiceNamePart;

        if (serviceName.equals(DEFAULT_NAME_VALUE)) {
            collaborateServiceNamePart = "join a new service";
            joinServiceNamePart = "";
        } else {
            collaborateServiceNamePart = format("collaborate on %s", serviceName);
            joinServiceNamePart = serviceName;
        }

        Map<String, String> personalisation = Map.of(
                "username", sender,
                "link", inviteUrl,
                "collaborateServiceNamePart", collaborateServiceNamePart,
                "joinServiceNamePart", joinServiceNamePart
        );

        return sendEmail(inviteExistingUserEmailTemplateId, email, personalisation);
    }

    public String sendLiveAccountCreatedEmail(String email, String serviceLiveAccountLink) {
        Map<String, String> personalisation = Map.of("service_live_account_link", serviceLiveAccountLink);
        return sendEmail(notifyConfiguration.getLiveAccountCreatedEmailTemplateId(), email, personalisation);
    }
    
    public void sendStripeDisputeCreatedEmail(Set<String> emailAddresses, Map<String, String> personalisation) {
        emailAddresses.forEach(email -> sendEmail(stripeDisputeCreatedEmailTemplateId, email, personalisation, notifyEmailReplyToSupportId));
    }

    public void sendStripeDisputeLostEmail(Set<String> emailAddresses, Map<String, String> personalisation) {
        boolean hasFee = personalisation.containsKey("disputeFee");
        String templateId = hasFee ? stripeDisputeLostAndServiceChargedEmailTemplateId : stripeDisputeLostEmailTemplateId;

        emailAddresses.forEach(email -> sendEmail(templateId, email, personalisation, notifyEmailReplyToSupportId));
    }

    public void sendStripeDisputeEvidenceSubmittedEmail(Set<String> emailAddresses, Map<String, String> personalisation) {
        emailAddresses.forEach(email -> sendEmail(stripeDisputeEvidenceSubmittedEmailTemplateId, email, personalisation, notifyEmailReplyToSupportId));
    }

    public void sendStripeDisputeWonEmail(Set<String> emailAddresses, Map<String, String> personalisation) {
        emailAddresses.forEach(email -> sendEmail(stripeDisputeWonEmailTemplateId, email, personalisation, notifyEmailReplyToSupportId));
    }

    public String sendEmail(final String templateId, final String email, final Map<String, String> personalisation) {
       return sendEmail(templateId, email, personalisation, null);
    }
    
    public String sendEmail(final String templateId, final String email, final Map<String, String> personalisation, final String emailReplyToId) {
        Stopwatch responseTimeStopwatch = Stopwatch.createStarted();
        try {
            SendEmailResponse response = notifyClientProvider.get().sendEmail(templateId, email, personalisation, null, emailReplyToId);
            return response.getNotificationId().toString();
        } catch (Exception e) {
            metricRegistry.counter("notify-operations.email.failures").inc();
            LOGGER.info("Error sending email: {}", e.getMessage());
            throw userNotificationError(e);
        } finally {
            responseTimeStopwatch.stop();
            metricRegistry.histogram("notify-operations.email.response_time").update(responseTimeStopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    private String resolveOtpNotifySmsTemplateId(OtpNotifySmsTemplateId otpNotifySmsTemplateId) {
        switch (otpNotifySmsTemplateId) {
            case SIGN_IN:
                return signInOtpSmsTemplateId;
            case CHANGE_SIGN_IN_2FA_TO_SMS:
                return changeSignIn2faToSmsOtpSmsTemplateId;
            case SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE:
                return selfInitiatedCreateUserAndServiceOtpSmsTemplateId;
            case CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE:
                return createUserInResponseToInvitationToServiceOtpSmsTemplateId;
            default:
                throw new IllegalArgumentException("Unrecognised OtpNotifySmsTemplateId: " + otpNotifySmsTemplateId.name());
        }
    }

    public enum OtpNotifySmsTemplateId {
        SIGN_IN, CHANGE_SIGN_IN_2FA_TO_SMS, SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE, CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE;
    }

    public Instant getEmailNotificationsForLivePaymentsDisputeUpdatesFrom() {
        return emailNotificationsForLivePaymentsDisputeUpdatesFrom;
    }

    public Instant getEmailNotificationsForTestPaymentsDisputeUpdatesFrom() {
        return emailNotificationsForTestPaymentsDisputeUpdatesFrom;
    }
}
