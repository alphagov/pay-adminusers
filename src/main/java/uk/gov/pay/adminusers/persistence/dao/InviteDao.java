package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

import javax.persistence.EntityManager;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Transactional
public class InviteDao extends JpaDao<InviteEntity> {

    public static final int FP_CODE_VALIDITY_IN_MINUTES = 90;

    @Inject
    protected InviteDao(Provider<EntityManager> entityManager) {
        super(entityManager, InviteEntity.class);
    }


    /*public Optional<ForgottenPasswordEntity> findNonExpiredByCode(String code) {
        String query = "SELECT fp FROM ForgottenPasswordEntity fp " +
                "WHERE fp.code = :code AND fp.date >= :expiry";

        ZonedDateTime expiryDateTime = ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(FP_CODE_VALIDITY_IN_MINUTES);

        return entityManager.get()
                .createQuery(query, ForgottenPasswordEntity.class)
                .setParameter("code", code)
                .setParameter("expiry", expiryDateTime)
                .getResultList().stream().findFirst();
    }*/
}
