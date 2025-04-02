package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Date.from;
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

    public Optional<UserEntity> findByEmail(String email) {
        String query = "SELECT u FROM UserEntity u " +
                "WHERE LOWER(u.email) = LOWER(:email)";

        return entityManager.get()
                .createQuery(query, UserEntity.class)
                .setParameter("email", email)
                .getResultList().stream().findFirst();
    }

    public List<UserEntity> findByServiceId(Integer serviceId, RoleName roleName) {

        String query = "SELECT s FROM ServiceRoleEntity s ";
        
        if (roleName != null) {
            query += " LEFT JOIN RoleEntity re ON s.role = re ";
        }

        query += " WHERE s.service.id = :serviceId ";
        
        if (roleName != null) {
            query += " AND re.roleName = :roleName ";
        }
        
        query += " ORDER BY s.user.email";

        TypedQuery<ServiceRoleEntity> typedQuery = entityManager.get()
                .createQuery(query, ServiceRoleEntity.class)
                .setParameter("serviceId", serviceId);
        
        if (roleName != null) {
            typedQuery.setParameter("roleName", roleName);
        }
        
        return typedQuery.getResultList().stream()
                .map(ServiceRoleEntity::getUser)
                .toList();
    }

    public List<UserEntity> findByServiceId(Integer serviceId) {
        return findByServiceId(serviceId, null);
    }

    public int deleteUsersNotAssociatedWithAnyService(Instant deleteRecordsBeforeDate) {
        String query = "DELETE FROM users u" +
                " WHERE u.id in (" +
                "    SELECT u.id FROM users u" +
                "     LEFT OUTER JOIN user_services_roles usr " +
                "     ON u.id = usr.user_id" +
                "    WHERE usr.user_id is null" +
                "      AND (last_logged_in_at < ?1 OR (\"createdAt\" < ?2 AND last_logged_in_at IS null))" +
                " )";

        return entityManager.get()
                .createNativeQuery(query)
                .setParameter(1, from(deleteRecordsBeforeDate))
                .setParameter(2, from(deleteRecordsBeforeDate))
                .executeUpdate();
    }
}
