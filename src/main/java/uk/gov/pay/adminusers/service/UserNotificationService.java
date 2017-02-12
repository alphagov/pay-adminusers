package uk.gov.pay.adminusers.service;


import com.google.inject.Inject;
import uk.gov.service.notify.NotificationResponse;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.google.common.collect.Maps.newHashMap;

public class UserNotificationService {

    private final ExecutorService executorService;
    private final NotifyClientProvider notifyClientProvider;
    private final String secondFactorSmsTemplateId;

    @Inject
    public UserNotificationService(ExecutorService executorService, NotifyClientProvider notificationClientProvider, String secondFactorSmsTemplateId) {
        this.executorService = executorService;
        this.notifyClientProvider = notificationClientProvider;
        this.secondFactorSmsTemplateId = secondFactorSmsTemplateId;
    }

    public CompletableFuture<String> sendSecondFactorPasscodeSms(String phoneNumber, int passcode) {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<String, String> personalisation = newHashMap();
            personalisation.put("code", String.valueOf(passcode));
            try {
                NotificationResponse notificationResponse = notifyClientProvider.get().sendSms(secondFactorSmsTemplateId, phoneNumber, personalisation);
                return notificationResponse.getNotificationId();
            } catch (Exception e) {
                throw AdminUsersExceptions.userNotificationError(e);
            }
        }, executorService);
    }
}
