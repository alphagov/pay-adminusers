package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.exception.GovUkPayAgreementNotSignedException;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.persistence.dao.GovUkPayAgreementDao;
import uk.gov.pay.adminusers.persistence.entity.GovUkPayAgreementEntity;

import static javax.ws.rs.core.UriBuilder.fromUri;

public class SendLiveAccountCreatedEmailService {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(SendLiveAccountCreatedEmailService.class);
    private static final String SELFSERVICE_LIVE_ACCOUNT_PATH = "live-account";
    
    private final GovUkPayAgreementDao govUkPayAgreementDao;
    private final NotificationService notificationService;
    private final String selfserviceServicesUrl;
    
    @Inject
    public SendLiveAccountCreatedEmailService(GovUkPayAgreementDao govUkPayAgreementDao,
                                              NotificationService notificationService,
                                              AdminUsersConfig config) {
        this.govUkPayAgreementDao = govUkPayAgreementDao;
        this.notificationService = notificationService;
        this.selfserviceServicesUrl = config.getLinks().getSelfserviceServicesUrl();
    }

    public void sendEmail(String serviceExternalId) {
        GovUkPayAgreementEntity agreement = govUkPayAgreementDao.findByExternalServiceId(serviceExternalId)
                .orElseThrow(GovUkPayAgreementNotSignedException::new);

        String serviceLiveAccountUrl = fromUri(selfserviceServicesUrl)
                .path(serviceExternalId)
                .path(SELFSERVICE_LIVE_ACCOUNT_PATH)
                .build()
                .toString();

        notificationService.sendLiveAccountCreatedEmail(agreement.getEmail(), serviceLiveAccountUrl)
                .thenAcceptAsync(notificationId -> LOGGER.info("Sent service is live email successfully, notification id [{}]", notificationId))
                .exceptionally(exception -> {
                    LOGGER.error("Error sending service is live email", exception);
                    return null;
                });
    }
}
