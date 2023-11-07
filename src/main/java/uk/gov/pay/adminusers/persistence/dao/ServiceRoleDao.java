package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;

import javax.persistence.EntityManager;

@Transactional
public class ServiceRoleDao extends JpaDao<ServiceRoleEntity> {

    @Inject
    /* default */ ServiceRoleDao(Provider<EntityManager> entityManager) {
        super(entityManager, ServiceRoleEntity.class);
    }

    public void removeUsersFromService(Integer serviceId) {
        entityManager.get()
                .createQuery("DELETE from ServiceRoleEntity where service.id = :serviceId")
                .setParameter("serviceId", serviceId)
                .executeUpdate();
    }
}
