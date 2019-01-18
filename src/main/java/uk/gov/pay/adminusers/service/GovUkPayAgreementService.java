package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.GovUkPayAgreement;
import uk.gov.pay.adminusers.persistence.dao.GovUkPayAgreementDao;
import uk.gov.pay.adminusers.persistence.entity.GovUkPayAgreementEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import java.time.ZonedDateTime;
import java.util.Optional;

import static java.lang.String.format;

public class GovUkPayAgreementService {

    private final static Logger LOGGER = LoggerFactory.getLogger(GovUkPayAgreementService.class);
    private final GovUkPayAgreementDao agreementDao;
    
    @Inject
    public GovUkPayAgreementService(GovUkPayAgreementDao govUkPayAgreementDao) {
        this.agreementDao = govUkPayAgreementDao;
    }


    public Optional<GovUkPayAgreement> findGovUkPayAgreementByServiceId(String serviceExternalId) {
        return agreementDao.findByExternalServiceId(serviceExternalId)
                .map(GovUkPayAgreementEntity::toGovUkPayAgreement);
    }

    public void doCreate(ServiceEntity serviceEntity, String email, ZonedDateTime agreementTime) {
        LOGGER.info(format("Creating GOV.UK Pay agreement for service %s", serviceEntity.getExternalId()));
        GovUkPayAgreementEntity agreementEntity = new GovUkPayAgreementEntity(email, agreementTime);
        agreementEntity.setService(serviceEntity);
        agreementDao.persist(agreementEntity);
    }
}
