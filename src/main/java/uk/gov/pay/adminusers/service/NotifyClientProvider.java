package uk.gov.pay.adminusers.service;

import uk.gov.pay.adminusers.app.config.NotifyConfiguration;
import uk.gov.pay.adminusers.model.PaymentType;
import uk.gov.service.notify.NotificationClient;

public class NotifyClientProvider {

    private NotifyConfiguration configuration;

    public NotifyClientProvider(NotifyConfiguration configuration) {
        this.configuration = configuration;
    }

    public NotificationClient get() {
        String apiKey = configuration.getCardApiKey();

        return new NotificationClient(apiKey, configuration.getNotificationBaseURL(), null);
    }

}
