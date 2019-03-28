package uk.gov.pay.adminusers.service;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;
import uk.gov.pay.adminusers.app.config.NotifyConfiguration;
import uk.gov.pay.adminusers.app.config.NotifyDirectDebitConfiguration;
import uk.gov.pay.adminusers.model.PaymentType;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static uk.gov.pay.adminusers.model.PaymentType.CARD;
import static uk.gov.pay.adminusers.model.Service.DEFAULT_NAME_VALUE;

public class NotificationService {

    private final ExecutorService executorService;
    private final NotifyClientProvider notifyClientProvider;
    private final MetricRegistry metricRegistry;
    private final NotifyConfiguration notifyConfiguration;
    private final NotifyDirectDebitConfiguration notifyDirectDebitConfiguration;

    private final String secondFactorSmsTemplateId;
    private final String inviteEmailTemplateId;
    private final String forgottenPasswordEmailTemplateId;
    private final String inviteExistingUserEmailTemplateId;

    public NotificationService(ExecutorService executorService,
                               NotifyConfiguration notifyConfiguration,
                               NotifyDirectDebitConfiguration notifyDirectDebitConfiguration,
                               MetricRegistry metricRegistry) {
        this.executorService = executorService;
        this.notifyConfiguration = notifyConfiguration;
        this.notifyDirectDebitConfiguration = notifyDirectDebitConfiguration;

        this.notifyClientProvider = new NotifyClientProvider(notifyConfiguration);
        this.secondFactorSmsTemplateId = notifyConfiguration.getSecondFactorSmsTemplateId();
        this.inviteEmailTemplateId = notifyConfiguration.getInviteUserEmailTemplateId();
        this.inviteExistingUserEmailTemplateId = notifyConfiguration.getInviteUserExistingEmailTemplateId();
        this.forgottenPasswordEmailTemplateId = notifyConfiguration.getForgottenPasswordEmailTemplateId();

        this.metricRegistry = metricRegistry;
    }

    NotifyConfiguration getNotifyConfiguration() {
        return notifyConfiguration;
    }

    NotifyDirectDebitConfiguration getNotifyDirectDebitConfiguration() {
        return notifyDirectDebitConfiguration;
    }

    CompletableFuture<String> sendSecondFactorPasscodeSms(String phoneNumber, String passcode) {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<String, String> personalisation = newHashMap();
            personalisation.put("code", passcode);
            Stopwatch responseTimeStopwatch = Stopwatch.createStarted();
            try {
                SendSmsResponse response = notifyClientProvider.get(CARD).sendSms(secondFactorSmsTemplateId, TelephoneNumberUtility.formatToE164(phoneNumber), personalisation, null);
                return response.getNotificationId().toString();
            } catch (Exception e) {
                metricRegistry.counter("notify-operations.sms.failures").inc();
                throw AdminUsersExceptions.userNotificationError(e);
            } finally {
                responseTimeStopwatch.stop();
                metricRegistry.histogram("notify-operations.sms.response_time").update(responseTimeStopwatch.elapsed(TimeUnit.MILLISECONDS));
            }
        }, executorService);
    }

    CompletableFuture<String> sendInviteEmail(String sender, String email, String inviteUrl) {
        HashMap<String, String> personalisation = newHashMap();
        personalisation.put("username", sender);
        personalisation.put("link", inviteUrl);
        return sendEmailAsync(CARD, inviteEmailTemplateId, email, personalisation);
    }

    CompletableFuture<String> sendServiceInviteEmail(String email, String inviteUrl) {
        HashMap<String, String> personalisation = newHashMap();
        personalisation.put("name", email);
        personalisation.put("link", inviteUrl);
        return sendEmailAsync(CARD, notifyConfiguration.getInviteServiceEmailTemplateId(), email, personalisation);
    }

    CompletableFuture<String> sendForgottenPasswordEmail(String email, String forgottenPasswordUrl) {
        HashMap<String, String> personalisation = newHashMap();
        personalisation.put("code", forgottenPasswordUrl);
        return sendEmailAsync(CARD, forgottenPasswordEmailTemplateId, email, personalisation);
    }

    CompletionStage<String> sendServiceInviteUserExistsEmail(String email, String signInLink, String forgottenPasswordLink, String feedbackLink) {
        HashMap<String, String> personalisation = newHashMap();
        personalisation.put("signin_link", signInLink);
        personalisation.put("forgotten_password_link", forgottenPasswordLink);
        personalisation.put("feedback_link", feedbackLink);
        return sendEmailAsync(CARD, notifyConfiguration.getInviteServiceUserExistsEmailTemplateId(), email, personalisation);
    }

    CompletableFuture<String> sendServiceInviteUserDisabledEmail(String email, String supportUrl) {
        HashMap<String, String> personalisation = newHashMap();
        personalisation.put("feedback_link", supportUrl);
        return sendEmailAsync(CARD, notifyConfiguration.getInviteServiceUserDisabledEmailTemplateId(), email, personalisation);
    }

    CompletableFuture<String> sendInviteExistingUserEmail(String sender, String email, String inviteUrl, String serviceName) {
        String collaborateServiceNamePart, joinServiceNamePart = "";
        HashMap<String, String> personalisation = newHashMap();

        personalisation.put("username", sender);
        personalisation.put("link", inviteUrl);

        if (serviceName.equals(DEFAULT_NAME_VALUE)) {
            collaborateServiceNamePart = "join a new service";
        } else {
            collaborateServiceNamePart = format("collaborate on %s", serviceName);
            joinServiceNamePart = serviceName;
        }
        personalisation.put("collaborateServiceNamePart", collaborateServiceNamePart);
        personalisation.put("joinServiceNamePart", joinServiceNamePart);

        return sendEmailAsync(CARD, inviteExistingUserEmailTemplateId, email, personalisation);
    }

    CompletableFuture<String> sendLiveAccountCreatedEmail(String email, String serviceLiveAccountLink) {
        HashMap<String, String> personalisation = newHashMap();
        personalisation.put("service_live_account_link", serviceLiveAccountLink);
        return sendEmailAsync(CARD, notifyConfiguration.getLiveAccountCreatedEmailTemplateId(), email, personalisation);
    }

    public CompletableFuture<String> sendEmailAsync(PaymentType paymentType, final String templateId, final String email, final Map<String, String> personalisation) {
        return CompletableFuture.supplyAsync(() -> {
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
        }, executorService);
    }
}
