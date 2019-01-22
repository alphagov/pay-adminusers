package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.exception.ServiceNotFoundException;
import uk.gov.pay.adminusers.exception.StripeAgreementExistsException;
import uk.gov.pay.adminusers.model.StripeAgreement;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.StripeAgreementDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.StripeAgreementEntity;

import java.net.InetAddress;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.lang.String.format;

public class StripeAgreementService {

    private static Logger logger = LoggerFactory.getLogger(StripeAgreementService.class);

    private final StripeAgreementDao stripeAgreementDao;
    private ServiceDao serviceDao;

    @Inject
    public StripeAgreementService(StripeAgreementDao stripeAgreementDao,
                                  ServiceDao serviceDao) {
        this.stripeAgreementDao = stripeAgreementDao;
        this.serviceDao = serviceDao;
    }

    public Optional<StripeAgreement> findStripeAgreementByServiceId(String serviceExternalId) {
        return stripeAgreementDao.findByServiceExternalId(serviceExternalId)
                .map((StripeAgreementEntity::toStripeAgreement));
    }
    
    public void doCreate(String serviceExternalId, InetAddress ipAddress) {
        ServiceEntity serviceEntity = serviceDao.findByExternalId(serviceExternalId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceExternalId));
        
        if (stripeAgreementDao.findByServiceExternalId(serviceExternalId).isPresent()) {
            throw new StripeAgreementExistsException();
        }

        logger.info(format("Creating stripe agreement for service %s", serviceExternalId));
        ZonedDateTime agreementTime = ZonedDateTime.now(ZoneId.of("UTC"));
        stripeAgreementDao.persist(new StripeAgreementEntity(serviceEntity, ipAddress.getHostAddress(), agreementTime));
    }
}
