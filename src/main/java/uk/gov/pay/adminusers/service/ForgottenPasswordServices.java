package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;

import java.time.ZonedDateTime;
import java.util.Optional;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;

public class ForgottenPasswordServices {

    private static final Logger logger = PayLoggerFactory.getLogger(ForgottenPasswordServices.class);
    private final UserDao userDao;
    private final ForgottenPasswordDao forgottenPasswordDao;
    private final LinksBuilder linksBuilder;

    @Inject
    public ForgottenPasswordServices(UserDao userDao, ForgottenPasswordDao forgottenPasswordDao, LinksBuilder linksBuilder) {
        this.userDao = userDao;
        this.forgottenPasswordDao = forgottenPasswordDao;
        this.linksBuilder = linksBuilder;
    }

    public Optional<ForgottenPassword> create(String username) {
        return userDao.findByUsername(username)
                .map(userEntity -> {
                    ForgottenPasswordEntity forgottenPasswordEntity = new ForgottenPasswordEntity(newId(), ZonedDateTime.now(), userEntity);
                    forgottenPasswordDao.persist(forgottenPasswordEntity);
                    return Optional.of(linksBuilder.decorate(forgottenPasswordEntity.toForgottenPassword()));
                })
                .orElseGet(() -> {
                    logger.warn("Attempted forgotten password for non existent user {}", username);
                    return Optional.empty();
                });
    }

    public Optional<ForgottenPassword> findNonExpired(String code) {
        return forgottenPasswordDao.findNonExpiredByCode(code)
                .map(forgottenPasswordEntity -> Optional.of(linksBuilder.decorate(forgottenPasswordEntity.toForgottenPassword())))
                .orElseGet(() -> {
                    logger.warn("Attempted forgotten password GET for non-existent/expired code {}", code);
                    return Optional.empty();
                });
    }
}
