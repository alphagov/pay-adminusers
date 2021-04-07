package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableMap;

@Transactional
public class UserDao extends JpaDao<UserEntity> {

    @Inject
    public UserDao(Provider<EntityManager> entityManager) {
        super(entityManager, UserEntity.class);
    }

    public Optional<UserEntity> findByExternalId(String externalId) {
        String query = "SELECT u FROM UserEntity u " +
                "WHERE LOWER(u.externalId) = LOWER(:externalId)";

        return entityManager.get()
                .createQuery(query, UserEntity.class)
                .setParameter("externalId", externalId)
                .getResultList().stream().findFirst();
    }

    public List<UserEntity> findByExternalIds(List<String> externalIds) {
        String query = "SELECT u FROM UserEntity u WHERE LOWER(u.externalId) in :externalIds";

        List<String> lowerCaseExternalIds = externalIds.stream().map(String::toLowerCase).collect(toUnmodifiableList());

        return entityManager.get()
                .createQuery(query, UserEntity.class)
                .setParameter("externalIds", lowerCaseExternalIds)
                .getResultList();
    }
    
    public Map<String, List<String>> getAdminUserEmailsForGatewayAccountIds(List<String> gatewayAccountIds) {
        if (gatewayAccountIds.size() > 0) {
            String positionalParams = IntStream.rangeClosed(1, gatewayAccountIds.size()).mapToObj(Integer::toString)
                    .map(i -> "?" + i).collect(Collectors.joining(","));

            String query = "SELECT sga.gateway_account_id, users.email FROM service_gateway_accounts sga" +
                    " RIGHT JOIN user_services_roles usr" +
                    " ON usr.service_id = sga.service_id" +
                    " JOIN users ON users.id = usr.user_id" +
                    " JOIN roles ON roles.id = usr.role_id" +
                    " WHERE sga.gateway_account_id in (" + positionalParams + ")" +
                    " AND roles.name='admin'" +
                    " ORDER by sga.gateway_account_id";

            Query nativeQuery = entityManager.get().createNativeQuery(query);

            IntStream.rangeClosed(1, gatewayAccountIds.size()).forEach(i ->
                    nativeQuery.setParameter(i, gatewayAccountIds.get(i - 1)));

            List<Object[]> gatewayAccountIdsToAdminEmails = nativeQuery.getResultList();
            return gatewayAccountIdsToAdminEmails.stream()
                    .map(arrayOfObject -> new SimpleEntry<>((String) arrayOfObject[0], (String) arrayOfObject[1]))
                    .collect(groupingBy(SimpleEntry::getKey))
                    .entrySet()
                    .stream()
                    .collect(toUnmodifiableMap(
                            Map.Entry::getKey,
                            abstractMap -> abstractMap.getValue().stream().map(SimpleEntry::getValue).collect(toUnmodifiableList())));
        } else {
            return Map.of();
        }
    }

    public Optional<UserEntity> findByUsername(String username) {
        String query = "SELECT u FROM UserEntity u " +
                "WHERE LOWER(u.username) = LOWER(:username)";

        return entityManager.get()
                .createQuery(query, UserEntity.class)
                .setParameter("username", username)
                .getResultList().stream().findFirst();
    }

    public Optional<UserEntity> findByEmail(String email) {
        String query = "SELECT u FROM UserEntity u " +
                "WHERE LOWER(u.email) = LOWER(:email)";

        return entityManager.get()
                .createQuery(query, UserEntity.class)
                .setParameter("email", email)
                .getResultList().stream().findFirst();
    }

    public List<UserEntity> findByServiceId(Integer serviceId) {

        String query = "SELECT s FROM ServiceRoleEntity s " +
                "WHERE s.service.id = :serviceId ORDER BY s.user.username";

        return entityManager.get()
                .createQuery(query, ServiceRoleEntity.class)
                .setParameter("serviceId", serviceId)
                .getResultList().stream()
                .map(ServiceRoleEntity::getUser)
                .collect(toUnmodifiableList());
    }
}
