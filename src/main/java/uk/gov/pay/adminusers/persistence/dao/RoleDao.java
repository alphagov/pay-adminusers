package uk.gov.pay.adminusers.persistence.dao;


import com.google.inject.Inject;
import com.google.inject.Provider;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;

import javax.persistence.EntityManager;
import java.util.Optional;

/**
 * Not extending the generic JpaDao purposefully.
 * <p>This is to avoid inheriting persistence methods such as <b>persist, merge, etc</b>
 * which should never be used with ReadOnly RoleEntity.
 * </p>
 *     @see uk.gov.pay.adminusers.persistence.entity.RoleEntity
 */
public class RoleDao {

    private final Provider<EntityManager> entityManager;

    @Inject
    public RoleDao(Provider<EntityManager> entityManager){
        this.entityManager = entityManager;
    }

    public Optional<RoleEntity> findByRoleName(String roleName) {

        String query = "SELECT r FROM RoleEntity r " +
                "WHERE r.name = :roleName";

        return entityManager.get()
                .createQuery(query, RoleEntity.class)
                .setParameter("roleName", roleName)
                .getResultList().stream().findFirst();
    }
}
