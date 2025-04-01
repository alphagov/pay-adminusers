package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.time.ZonedDateTime;
import java.util.Optional;

import static java.lang.String.format;
import static jakarta.ws.rs.core.UriBuilder.fromUri;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class ForgottenPasswordServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgottenPasswordServices.class);
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

    public void create(String username) {
        Optional<UserEntity> userOptional = userDao.findByEmail(username);
        if (userOptional.isPresent()) {
            UserEntity userEntity = userOptional.get();
            ForgottenPasswordEntity forgottenPasswordEntity = new ForgottenPasswordEntity(randomUuid(), ZonedDateTime.now(), userEntity);
            forgottenPasswordDao.persist(forgottenPasswordEntity);
            String forgottenPasswordUrl = fromUri(selfserviceBaseUrl).path(SELFSERVICE_FORGOTTEN_PASSWORD_PATH).path(forgottenPasswordEntity.getCode()).build().toString();
            
            try {
                String notificationId = notificationService.sendForgottenPasswordEmail(userEntity.getEmail(), forgottenPasswordUrl);
                LOGGER.info("sent forgot password email successfully user [{}], notification id [{}]", userEntity.getExternalId(), notificationId);
            } catch (Exception e) {
                LOGGER.error(format("error sending forgotten password email for user [%s]", userEntity.getExternalId()), e);
            }
            
        } else {
            LOGGER.warn("Attempted forgotten password for non existent user {}", username);
            throw AdminUsersExceptions.notFoundException();
        }
    }

    public Optional<ForgottenPassword> findNonExpired(String code) {
        return forgottenPasswordDao.findNonExpiredByCode(code)
                .map(forgottenPasswordEntity -> Optional.of(linksBuilder.decorate(forgottenPasswordEntity.toForgottenPassword())))
                .orElseGet(() -> {
                    LOGGER.warn("Attempted forgotten password GET for non-existent/expired code {}", code);
                    return Optional.empty();
                });
    }
}
