package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import uk.gov.pay.adminusers.persistence.entity.UserMfaMethodEntity;

@Transactional
public class UserMfaMethodDao extends JpaDao<UserMfaMethodEntity> {

    @Inject
    public UserMfaMethodDao(Provider<EntityManager> entityManager) {
        super(entityManager, UserMfaMethodEntity.class);
    }

}
