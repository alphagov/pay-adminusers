package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Optional;

@Transactional
public class UserDao extends JpaDao<UserEntity> {

    @Inject
    public UserDao(Provider<EntityManager> entityManager) {
        super(entityManager, UserEntity.class);
    }

    public Optional<UserEntity> findByUsername(String username) {
        String query = "SELECT u FROM UserEntity u " +
                "WHERE u.username = :username";

        return entityManager.get()
                .createQuery(query, UserEntity.class)
                .setParameter("username", username)
                .getResultList().stream().findFirst();
    }

    public Optional<UserEntity> findByEmail(String email) {
        String query = "SELECT u FROM UserEntity u " +
                "WHERE u.email = :email";

        return entityManager.get()
                .createQuery(query, UserEntity.class)
                .setParameter("email", email)
                .getResultList().stream().findFirst();
    }

}
