package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

import javax.persistence.EntityManager;
import java.util.Optional;

@Transactional
public abstract class JpaDao<T> {

    /* default */ final Provider<EntityManager> entityManager;
    private final Class<T> persistenceClass;

    /* default */ JpaDao(Provider<EntityManager> entityManager, Class<T> persistenceClass) {
        this.entityManager = entityManager;
        this.persistenceClass = persistenceClass;
    }

    public void persist(final T object) {
        entityManager.get().persist(object);
    }

    public void remove(T object) {
        if (!entityManager.get().contains(object)) {
            T mergedObject = entityManager.get().merge(object);
            entityManager.get().remove(mergedObject);
        } else {
            entityManager.get().remove(object);
        }
    }

    public <ID> Optional<T> findById(final ID id) {
        return Optional.ofNullable(entityManager.get().find(persistenceClass, id));
    }

    public T merge(final T object) {
        return entityManager.get().merge(object);
    }
}
