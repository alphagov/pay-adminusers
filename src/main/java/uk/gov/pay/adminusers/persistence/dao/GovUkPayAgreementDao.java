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
    
    public Optional<GovUkPayAgreementEntity> findByServiceId(int serviceId) {

        String query = "SELECT s FROM GovUkPayAgreementEntity s " +
                "WHERE s.serviceId = :serviceId";

        return entityManager.get()
                .createQuery(query, GovUkPayAgreementEntity.class)
                .setParameter("serviceId", serviceId)
                .getResultStream()
                .findFirst();
    }
}
