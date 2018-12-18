package uk.gov.pay.adminusers.service;

import uk.gov.pay.adminusers.app.config.NotifyConfiguration;
import uk.gov.pay.adminusers.model.PaymentType;
import uk.gov.service.notify.NotificationClient;

import javax.net.ssl.SSLContext;

public class NotifyClientProvider {

    private final NotifyConfiguration configuration;
    private final SSLContext sslContext;

    NotifyClientProvider(NotifyConfiguration configuration, SSLContext sslContext) {
        this.configuration = configuration;
        this.sslContext = sslContext;
    }

    public NotificationClient get(PaymentType paymentType) {
        switch (paymentType) {
            case DIRECT_DEBIT:
                return new NotificationClient(configuration.getDirectDebitApiKey(), configuration.getNotificationBaseURL(), null, sslContext);
            case CARD:
            default:
                return new NotificationClient(configuration.getCardApiKey(), configuration.getNotificationBaseURL(), null, sslContext);
        }
    }

}
