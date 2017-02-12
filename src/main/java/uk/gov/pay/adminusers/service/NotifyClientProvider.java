package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import uk.gov.pay.adminusers.app.config.NotifyConfiguration;
import uk.gov.service.notify.NotificationClient;

public class NotifyClientProvider implements Provider<NotificationClient> {

    private NotifyConfiguration configuration;

    @Inject
    public NotifyClientProvider(NotifyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public NotificationClient get() {
        return new NotificationClient(configuration.getSecret(),
                configuration.getServiceId(),
                configuration.getNotificationBaseURL()
        );
    }

}
