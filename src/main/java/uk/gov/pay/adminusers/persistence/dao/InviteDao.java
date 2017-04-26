package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

import javax.persistence.EntityManager;
import java.util.Optional;

@Transactional
public class InviteDao extends JpaDao<InviteEntity> {

    @Inject
    protected InviteDao(Provider<EntityManager> entityManager) {
        super(entityManager, InviteEntity.class);
    }

    public Optional<InviteEntity> findByCode(String code) {

        String query = "SELECT invite FROM InviteEntity invite " +
                "WHERE invite.code = :code";

        return entityManager.get()
                .createQuery(query, InviteEntity.class)
                .setParameter("code", code)
                .getResultList().stream().findFirst();
    }

    public Optional<InviteEntity> findByEmail(String email) {

        String query = "SELECT invite FROM InviteEntity invite " +
                "WHERE invite.email = :email";

        return entityManager.get()
                .createQuery(query, InviteEntity.class)
                .setParameter("email", email)
                .getResultList().stream().findFirst();
    }
}
