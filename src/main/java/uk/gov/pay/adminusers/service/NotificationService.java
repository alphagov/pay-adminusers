package uk.gov.pay.adminusers.service;


import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Maps.newHashMap;

public class NotificationService {

    private final ExecutorService executorService;
    private final NotifyClientProvider notifyClientProvider;
    private final String secondFactorSmsTemplateId;
    private final String inviteEmailTemplateId;
    private final MetricRegistry metricRegistry;

    public NotificationService(ExecutorService executorService, 
                               NotifyClientProvider notificationClientProvider, 
                               String secondFactorSmsTemplateId, 
                               String inviteEmailTemplateId, 
                               MetricRegistry metricRegistry) {
        this.executorService = executorService;
        this.notifyClientProvider = notificationClientProvider;
        this.secondFactorSmsTemplateId = secondFactorSmsTemplateId;
        this.inviteEmailTemplateId = inviteEmailTemplateId;
        this.metricRegistry = metricRegistry;
    }

    public CompletableFuture<String> sendSecondFactorPasscodeSms(String phoneNumber, String passcode) {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<String, String> personalisation = newHashMap();
            personalisation.put("code", passcode);
            Stopwatch responseTimeStopwatch = Stopwatch.createStarted();
            try {
                SendSmsResponse response = notifyClientProvider.get().sendSms(secondFactorSmsTemplateId, phoneNumber, personalisation, null);
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

    public CompletableFuture<String> sendInviteEmail(String sender, String email, String inviteUrl) {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<String, String> personalisation = newHashMap();
            personalisation.put("username", sender);
            personalisation.put("link", inviteUrl);
            Stopwatch responseTimeStopwatch = Stopwatch.createStarted();
            try {
                SendEmailResponse response = notifyClientProvider.get().sendEmail(inviteEmailTemplateId, email, personalisation, null);
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
