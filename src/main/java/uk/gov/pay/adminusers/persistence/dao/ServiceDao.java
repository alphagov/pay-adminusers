package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.GatewayAccountEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Optional;

@Transactional
public class ServiceDao extends JpaDao<ServiceEntity> {

    @Inject
    public ServiceDao(Provider<EntityManager> entityManager) {
        super(entityManager, ServiceEntity.class);
    }

    public Optional<ServiceEntity> findByGatewayAccountId(String gatewayAccountId) {

        String query = "SELECT ga FROM GatewayAccountEntity ga " +
                "WHERE ga.gatewayAccountId = :gatewayAccountId";

        Optional<GatewayAccountEntity> gatewayAccount = entityManager.get()
                .createQuery(query, GatewayAccountEntity.class)
                .setParameter("gatewayAccountId", gatewayAccountId)
                .getResultList().stream().findFirst();

        if (gatewayAccount.isPresent()) {
            return Optional.of(gatewayAccount.get().getService());
        }
        return Optional.empty();
    }
}
