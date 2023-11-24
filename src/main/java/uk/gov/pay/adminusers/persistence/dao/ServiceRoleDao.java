package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;

import javax.persistence.EntityManager;
import java.util.List;

@Transactional
public class ServiceRoleDao extends JpaDao<ServiceRoleEntity> {

    @Inject
    /* default */ ServiceRoleDao(Provider<EntityManager> entityManager) {
        super(entityManager, ServiceRoleEntity.class);
    }

    public List<ServiceRoleEntity> findServiceUserRoles(Integer serviceId) {
        return entityManager.get()
                .createQuery("select sre from ServiceRoleEntity sre where sre.service.id = :serviceId")
                .setParameter("serviceId", serviceId)
                .getResultList();
    }
}
