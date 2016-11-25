package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

import javax.persistence.EntityManager;
import java.util.Optional;

@Transactional
public abstract class JpaDao<T> {

    protected final Provider<EntityManager> entityManager;

    protected JpaDao(Provider<EntityManager> entityManager) {
        this.entityManager = entityManager;
    }

    public void persist(final T object) {
        entityManager.get().persist(object);
    }

    public void remove(final T object) {
        entityManager.get().remove(object);
    }

    public <ID> Optional<T> findById(final Class<T> clazz, final ID id) {
        return Optional.ofNullable(entityManager.get().find(clazz, id));
    }

    public T merge(final T object) {
        return entityManager.get().merge(object);
    }
}
