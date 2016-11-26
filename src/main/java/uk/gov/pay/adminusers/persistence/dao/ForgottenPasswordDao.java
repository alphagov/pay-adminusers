package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;

import javax.persistence.EntityManager;
import java.util.Optional;

@Transactional
public class ForgottenPasswordDao extends JpaDao<ForgottenPasswordEntity> {

    @Inject
    protected ForgottenPasswordDao(Provider<EntityManager> entityManager) {
        super(entityManager, ForgottenPasswordEntity.class);
    }


    public Optional<ForgottenPasswordEntity> findByCode(String code) {
        String query = "SELECT fp FROM ForgottenPasswordEntity fp " +
                "WHERE fp.code = :code";

        return entityManager.get()
                .createQuery(query, ForgottenPasswordEntity.class)
                .setParameter("code", code)
                .getResultList().stream().findFirst();
    }
}
