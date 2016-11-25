package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.inject.Inject;
import javax.persistence.EntityManager;

@Transactional
public class UserDao extends JpaDao<UserEntity> {

    @Inject
    public UserDao(Provider<EntityManager> entityManager) {
        super(entityManager);
    }
}
