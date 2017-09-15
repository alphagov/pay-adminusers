package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        externalIds = externalIds.stream().map(String::toLowerCase).collect(Collectors.toList());

        return entityManager.get()
                .createQuery(query, UserEntity.class)
                .setParameter("externalIds", externalIds)
                .getResultList();
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
                .collect(Collectors.toList());
    }
}
