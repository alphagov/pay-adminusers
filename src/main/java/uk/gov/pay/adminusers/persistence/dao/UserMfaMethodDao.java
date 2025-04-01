package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.UserMfaMethodEntity;

import javax.inject.Inject;
import javax.persistence.EntityManager;

@Transactional
public class UserMfaMethodDao extends JpaDao<UserMfaMethodEntity> {

    @Inject
    public UserMfaMethodDao(Provider<EntityManager> entityManager) {
        super(entityManager, UserMfaMethodEntity.class);
    }

}
