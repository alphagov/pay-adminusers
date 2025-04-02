package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.GatewayAccountIdEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;

@Transactional
public class ServiceDao extends JpaDao<ServiceEntity> {

    @Inject
    public ServiceDao(Provider<EntityManager> entityManager) {
        super(entityManager, ServiceEntity.class);
    }

    public List<ServiceEntity> listAll() {
        String query = "SELECT s FROM ServiceEntity as s";
        return entityManager.get()
                .createQuery(query, ServiceEntity.class)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<ServiceEntity> findByENServiceName(String searchString) {
        String query = "SELECT * FROM services s WHERE s.id IN (SELECT service_id FROM service_names sn WHERE to_tsvector('english', sn.name) @@ plainto_tsquery('english', ?) AND sn.language = 'en')";
        return entityManager.get().createNativeQuery(query, ServiceEntity.class)
                .setParameter(1, searchString)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<ServiceEntity> findByServiceMerchantName(String searchString) {
        String query = "SELECT * FROM services s WHERE to_tsvector('english', s.merchant_name) @@ plainto_tsquery('english', ?)";
        return entityManager.get().createNativeQuery(query, ServiceEntity.class)
                .setParameter(1, searchString)
                .getResultList();
    }

    public Optional<ServiceEntity> findByGatewayAccountId(String gatewayAccountId) {

        String query = "SELECT ga FROM GatewayAccountIdEntity ga " +
                "WHERE ga.gatewayAccountId = :gatewayAccountId";

        Optional<GatewayAccountIdEntity> gatewayAccount = entityManager.get()
                .createQuery(query, GatewayAccountIdEntity.class)
                .setParameter("gatewayAccountId", gatewayAccountId)
                .getResultList().stream().findFirst();

        return gatewayAccount.map(GatewayAccountIdEntity::getService);
    }

    public Long countOfUsersWithRoleForService(String serviceExternalId, Integer roleId) {

        String query = "SELECT count(*) FROM user_services_roles usr WHERE usr.role_id=? AND usr.service_id = (SELECT srv.id FROM services srv WHERE srv.external_id = ?)";
        return (long) entityManager.get().createNativeQuery(query)
                .setParameter(1, roleId)
                .setParameter(2, serviceExternalId)
                .getSingleResult();
    }

    public boolean checkIfGatewayAccountsUsed(List<String> gatewayAccountsIds) {
        String query = "SELECT count(*) FROM service_gateway_accounts WHERE gateway_account_id IN (?)";
        long count = (long) entityManager.get().createNativeQuery(query)
                .setParameter(1, String.join(",", gatewayAccountsIds))
                .getSingleResult();
        return count > 0;
    }

    public Optional<ServiceEntity> findByExternalId(String serviceExternalId) {
        String query = "SELECT s FROM ServiceEntity as s WHERE s.externalId = :externalId";
        return entityManager.get()
                .createQuery(query, ServiceEntity.class)
                .setParameter("externalId", serviceExternalId)
                .getResultList()
                .stream()
                .findFirst();
    }

    public List<ServiceEntity> findServicesToCheckForArchiving(ZonedDateTime archiveServicesBeforeDate) {
        ZonedDateTime now = now(UTC);

        String query = "SELECT s FROM ServiceEntity as s" +
                " WHERE (s.createdDate <= :archiveServicesBeforeDate" +
                "         OR (s.createdDate is null and s.firstCheckedForArchivalDate is null)" +
                "         OR s.firstCheckedForArchivalDate <= :archiveServicesBeforeDate)" +
                "   AND (s.skipCheckingForArchivalUntilDate is null " +
                "        OR s.skipCheckingForArchivalUntilDate <= :now)" +
                "   AND NOT s.archived";

        return entityManager.get()
                .createQuery(query, ServiceEntity.class)
                .setParameter("archiveServicesBeforeDate", archiveServicesBeforeDate)
                .setParameter("now", now)
                .getResultList();
    }
}
