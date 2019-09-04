package uk.gov.pay.adminusers.service;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;
import uk.gov.pay.adminusers.app.config.NotifyConfiguration;
import uk.gov.pay.adminusers.app.config.NotifyDirectDebitConfiguration;
import uk.gov.pay.adminusers.model.PaymentType;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.model.PaymentType.CARD;
import static uk.gov.pay.adminusers.model.Service.DEFAULT_NAME_VALUE;

public class NotificationService {

    private final NotifyClientProvider notifyClientProvider;
    private final MetricRegistry metricRegistry;
    private final NotifyConfiguration notifyConfiguration;
    private final NotifyDirectDebitConfiguration notifyDirectDebitConfiguration;

    private final String secondFactorSmsTemplateId;
    private final String inviteEmailTemplateId;
    private final String forgottenPasswordEmailTemplateId;
    private final String inviteExistingUserEmailTemplateId;

    public NotificationService(NotifyConfiguration notifyConfiguration,
                               NotifyDirectDebitConfiguration notifyDirectDebitConfiguration,
                               MetricRegistry metricRegistry) {
        this.notifyConfiguration = notifyConfiguration;
        this.notifyDirectDebitConfiguration = notifyDirectDebitConfiguration;

        this.notifyClientProvider = new NotifyClientProvider(notifyConfiguration);
        this.secondFactorSmsTemplateId = notifyConfiguration.getSecondFactorSmsTemplateId();
        this.inviteEmailTemplateId = notifyConfiguration.getInviteUserEmailTemplateId();
        this.inviteExistingUserEmailTemplateId = notifyConfiguration.getInviteUserExistingEmailTemplateId();
        this.forgottenPasswordEmailTemplateId = notifyConfiguration.getForgottenPasswordEmailTemplateId();

        this.metricRegistry = metricRegistry;
    }

    public NotifyDirectDebitConfiguration getNotifyDirectDebitConfiguration() {
        return notifyDirectDebitConfiguration;
    }

    public String sendSecondFactorPasscodeSms(String phoneNumber, String passcode) {
        Stopwatch responseTimeStopwatch = Stopwatch.createStarted();
        try {
            SendSmsResponse response = notifyClientProvider.get(CARD).sendSms(secondFactorSmsTemplateId, TelephoneNumberUtility.formatToE164(phoneNumber), Map.of("code", passcode), null);
            return response.getNotificationId().toString();
        } catch (Exception e) {
            metricRegistry.counter("notify-operations.sms.failures").inc();
            throw AdminUsersExceptions.userNotificationError(e);
        } finally {
            responseTimeStopwatch.stop();
            metricRegistry.histogram("notify-operations.sms.response_time").update(responseTimeStopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    public String sendInviteEmail(String sender, String email, String inviteUrl) {
        Map<String, String> personalisation = Map.of(
                "username", sender,
                "link", inviteUrl);
        return sendEmail(CARD, inviteEmailTemplateId, email, personalisation);
    }

    public String sendServiceInviteEmail(String email, String inviteUrl) {
        Map<String, String> personalisation = Map.of(
                "name", email,
                "link", inviteUrl);
        return sendEmail(CARD, notifyConfiguration.getInviteServiceEmailTemplateId(), email, personalisation);
    }

    public String sendForgottenPasswordEmail(String email, String forgottenPasswordUrl) {
        Map<String, String> personalisation = Map.of("code", forgottenPasswordUrl);
        return sendEmail(CARD, forgottenPasswordEmailTemplateId, email, personalisation);
    }

    public String sendServiceInviteUserExistsEmail(String email, String signInLink, String forgottenPasswordLink, String feedbackLink) {
        Map<String, String> personalisation = Map.of(
                "signin_link", signInLink,
                "forgotten_password_link", forgottenPasswordLink,
                "feedback_link", feedbackLink);
        return sendEmail(CARD, notifyConfiguration.getInviteServiceUserExistsEmailTemplateId(), email, personalisation);
    }

    public String sendServiceInviteUserDisabledEmail(String email, String supportUrl) {
        Map<String, String> personalisation = Map.of("feedback_link", supportUrl);
        return sendEmail(CARD, notifyConfiguration.getInviteServiceUserDisabledEmailTemplateId(), email, personalisation);
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

        return sendEmail(CARD, inviteExistingUserEmailTemplateId, email, personalisation);
    }

    public String sendLiveAccountCreatedEmail(String email, String serviceLiveAccountLink) {
        Map<String, String> personalisation = Map.of("service_live_account_link", serviceLiveAccountLink);
        return sendEmail(CARD, notifyConfiguration.getLiveAccountCreatedEmailTemplateId(), email, personalisation);
    }

    public String sendEmail(PaymentType paymentType, final String templateId, final String email, final Map<String, String> personalisation) {
        Stopwatch responseTimeStopwatch = Stopwatch.createStarted();
        try {
            SendEmailResponse response = notifyClientProvider.get(paymentType).sendEmail(templateId, email, personalisation, null);
            return response.getNotificationId().toString();
        } catch (Exception e) {
            metricRegistry.counter("notify-operations.email.failures").inc();
            throw AdminUsersExceptions.userNotificationError(e);
        } finally {
            responseTimeStopwatch.stop();
            metricRegistry.histogram("notify-operations.email.response_time").update(responseTimeStopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }
}
