package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;
import jakarta.persistence.EntityManager;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Transactional
public class ForgottenPasswordDao extends JpaDao<ForgottenPasswordEntity> {

    private final Integer forgottenPasswordExpiryMinutes;

    @Inject
    protected ForgottenPasswordDao(Provider<EntityManager> entityManager,
                                   @Named("FORGOTTEN_PASSWORD_EXPIRY_MINUTES") Integer forgottenPasswordExpiryMinutes) {
        super(entityManager, ForgottenPasswordEntity.class);
        this.forgottenPasswordExpiryMinutes = forgottenPasswordExpiryMinutes;
    }

    public Optional<ForgottenPasswordEntity> findNonExpiredByCode(String code) {
        String query = "SELECT fp FROM ForgottenPasswordEntity fp " +
                "WHERE fp.code = :code AND fp.date >= :expiry";

        ZonedDateTime expiryDateTime = ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(forgottenPasswordExpiryMinutes);

        return entityManager.get()
                .createQuery(query, ForgottenPasswordEntity.class)
                .setParameter("code", code)
                .setParameter("expiry", expiryDateTime)
                .getResultList().stream().findFirst();
    }
}
