package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.time.ZonedDateTime;
import java.util.Optional;

import static java.lang.String.format;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;

public class ForgottenPasswordServices {

    private static final Logger logger = PayLoggerFactory.getLogger(ForgottenPasswordServices.class);
    private static final String SELFSERVICE_FORGOTTEN_PASSWORD_PATH = "reset-password";

    private final UserDao userDao;
    private final ForgottenPasswordDao forgottenPasswordDao;
    private final LinksBuilder linksBuilder;
    private final NotificationService notificationService;
    private final String selfserviceBaseUrl;

    @Inject
    public ForgottenPasswordServices(UserDao userDao, ForgottenPasswordDao forgottenPasswordDao, LinksBuilder linksBuilder, NotificationService notificationService, AdminUsersConfig config) {
        this.userDao = userDao;
        this.forgottenPasswordDao = forgottenPasswordDao;
        this.linksBuilder = linksBuilder;
        this.notificationService = notificationService;
        this.selfserviceBaseUrl = config.getLinks().getSelfserviceUrl();
    }

    @Deprecated
    public Optional<ForgottenPassword> createWithoutNotification(String username) {
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

    public void create(String username) {
        Optional<UserEntity> userOptional = userDao.findByUsername(username);
        if (userOptional.isPresent()) {
            UserEntity userEntity = userOptional.get();
            ForgottenPasswordEntity forgottenPasswordEntity = new ForgottenPasswordEntity(newId(), ZonedDateTime.now(), userEntity);
            forgottenPasswordDao.persist(forgottenPasswordEntity);
            String forgottenPasswordUrl = fromUri(selfserviceBaseUrl).path(SELFSERVICE_FORGOTTEN_PASSWORD_PATH).path(forgottenPasswordEntity.getCode()).build().toString();
            notificationService.sendForgottenPasswordEmail(username, forgottenPasswordUrl)
                    .thenAcceptAsync(notificationId -> logger.info("sent forgot password email successfully user [{}], notification id [{}]", userEntity.getExternalId(), notificationId))
                    .exceptionally(exception -> {
                        logger.error(format("error sending forgotten password email for user [%s]", userEntity.getExternalId()), exception);
                        return null;
                    });
        } else {
            logger.warn("Attempted forgotten password for non existent user {}", username);
            throw AdminUsersExceptions.notFoundException();
        }
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
