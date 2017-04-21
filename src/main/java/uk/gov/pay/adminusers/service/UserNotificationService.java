package uk.gov.pay.adminusers.service;


import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;
import uk.gov.service.notify.SendSmsResponse;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Maps.newHashMap;

public class UserNotificationService {

    private final ExecutorService executorService;
    private final NotifyClientProvider notifyClientProvider;
    private final String secondFactorSmsTemplateId;
    private final MetricRegistry metricRegistry;

    public UserNotificationService(ExecutorService executorService, NotifyClientProvider notificationClientProvider, String secondFactorSmsTemplateId, MetricRegistry metricRegistry) {
        this.executorService = executorService;
        this.notifyClientProvider = notificationClientProvider;
        this.secondFactorSmsTemplateId = secondFactorSmsTemplateId;
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
}
