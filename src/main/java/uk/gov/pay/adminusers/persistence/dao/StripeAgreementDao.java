package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.StripeAgreementEntity;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Optional;

@Transactional
public class StripeAgreementDao extends JpaDao<StripeAgreementEntity> {

    @Inject
    public StripeAgreementDao(Provider<EntityManager> entityManager) {
        super(entityManager, StripeAgreementEntity.class);
    }


    public Optional<StripeAgreementEntity> findByServiceExternalId(String serviceExternalId) {

        String query = "SELECT s FROM StripeAgreementEntity s " +
                "WHERE s.service.externalId = :serviceExternalId";

        return entityManager.get()
                .createQuery(query, StripeAgreementEntity.class)
                .setParameter("serviceExternalId", serviceExternalId)
                .getResultStream()
                .findFirst();
    }
}
