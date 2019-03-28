package uk.gov.pay.adminusers.service;

import uk.gov.pay.adminusers.app.config.NotifyConfiguration;
import uk.gov.pay.adminusers.model.PaymentType;
import uk.gov.service.notify.NotificationClient;

import static uk.gov.pay.adminusers.model.PaymentType.DIRECT_DEBIT;

public class NotifyClientProvider {

    private NotifyConfiguration configuration;

    NotifyClientProvider(NotifyConfiguration configuration) {
        this.configuration = configuration;
    }

    public NotificationClient get(PaymentType paymentType) {
        String apiKey = paymentType == DIRECT_DEBIT
                ? configuration.getDirectDebitApiKey()
                : configuration.getCardApiKey();

        return new NotificationClient(apiKey, configuration.getNotificationBaseURL(), null);
    }

}
