package uk.gov.pay.adminusers.persistence.dao;

import com.google.common.base.Joiner;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.GatewayAccountIdEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

@Transactional
public class ServiceDao extends JpaDao<ServiceEntity> {

    @Inject
    public ServiceDao(Provider<EntityManager> entityManager) {
        super(entityManager, ServiceEntity.class);
    }

    public Optional<ServiceEntity> findByGatewayAccountId(String gatewayAccountId) {

        String query = "SELECT ga FROM GatewayAccountIdEntity ga " +
                "WHERE ga.gatewayAccountId = :gatewayAccountId";

        Optional<GatewayAccountIdEntity> gatewayAccount = entityManager.get()
                .createQuery(query, GatewayAccountIdEntity.class)
                .setParameter("gatewayAccountId", gatewayAccountId)
                .getResultList().stream().findFirst();

        if (gatewayAccount.isPresent()) {
            return Optional.of(gatewayAccount.get().getService());
        }
        return Optional.empty();
    }

    public Long countOfRolesForService(Integer serviceId, Integer roleId) {

        String query = "SELECT count(*) FROM user_services_roles usr WHERE usr.service_id=? AND usr.role_id=?";
        return (long) entityManager.get().createNativeQuery(query)
                .setParameter(1, serviceId)
                .setParameter(2, roleId)
                .getSingleResult();
    }

    public boolean checkIfGatewayAccountsUsed(List<String> gatewayAccountsIds) {
        String query = "SELECT count(*) FROM service_gateway_accounts WHERE gateway_account_id IN (?)";
        String idsString = Joiner.on(",").join(gatewayAccountsIds);
        long count = (long) entityManager.get().createNativeQuery(query)
                .setParameter(1, idsString)
                .getSingleResult();
        return count > 0;
    }
}
