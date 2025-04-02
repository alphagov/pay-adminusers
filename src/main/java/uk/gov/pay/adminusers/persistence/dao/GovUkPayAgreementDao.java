package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.GovUkPayAgreementEntity;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Optional;

@Transactional
public class GovUkPayAgreementDao extends JpaDao<GovUkPayAgreementEntity> {
    
    @Inject
    public GovUkPayAgreementDao(Provider<EntityManager> entityManager) {
        super(entityManager, GovUkPayAgreementEntity.class);
    }
    
    public Optional<GovUkPayAgreementEntity> findByExternalServiceId(String externalServiceId) {

        String query = "SELECT agreement FROM GovUkPayAgreementEntity agreement " +
                "WHERE agreement.service.externalId  = :externalServiceId";

        return entityManager.get()
                .createQuery(query, GovUkPayAgreementEntity.class)
                .setParameter("externalServiceId", externalServiceId)
                .getResultStream()
                .findFirst();
    }
}
