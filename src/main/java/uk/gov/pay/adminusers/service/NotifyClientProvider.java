package uk.gov.pay.adminusers.service;

import com.google.inject.Provider;
import uk.gov.pay.adminusers.app.config.NotifyConfiguration;
import uk.gov.service.notify.NotificationClient;

import javax.net.ssl.SSLContext;

public class NotifyClientProvider implements Provider<NotificationClient> {

    private NotifyConfiguration configuration;
    private final SSLContext sslContext;

    public NotifyClientProvider(NotifyConfiguration configuration, SSLContext sslContext) {
        this.configuration = configuration;
        this.sslContext = sslContext;
    }

    @Override
    public NotificationClient get() {
        return new NotificationClient(configuration.getApiKey(), configuration.getNotificationBaseURL(), null, sslContext);
    }
}
