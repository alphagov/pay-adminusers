package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.PatchRequest;
import uk.gov.pay.adminusers.model.SecondFactorToken;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_DISABLED;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_SESSION_VERSION;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_TELEPHONE_NUMBER;

public class UserServices {

    private static Logger logger = PayLoggerFactory.getLogger(UserServices.class);

    private final UserDao userDao;
    private final PasswordHasher passwordHasher;
    private final LinksBuilder linksBuilder;
    private final Integer loginAttemptCap;
    private final NotificationService notificationService;
    private final SecondFactorAuthenticator secondFactorAuthenticator;

    @Inject
    public UserServices(UserDao userDao,
                        PasswordHasher passwordHasher,
                        LinksBuilder linksBuilder,
                        @Named("LOGIN_ATTEMPT_CAP") Integer loginAttemptCap,
                        Provider<NotificationService> userNotificationService, SecondFactorAuthenticator secondFactorAuthenticator) {
        this.userDao = userDao;
        this.passwordHasher = passwordHasher;
        this.linksBuilder = linksBuilder;
        this.loginAttemptCap = loginAttemptCap;
        this.notificationService = userNotificationService.get();
        this.secondFactorAuthenticator = secondFactorAuthenticator;
    }

    /**
     * validates given username and password against persisted users
     * <p> on successful authentication, user's login count is reset to <b>0</b></p>
     * <p> on authentication failure, user's login count is increment by <b>1</b></p>
     *
     * @param username
     * @param password
     * @return {@link User} wrapped in an Optional if a matching user found. Otherwise an Optional.empty()
     * @throws javax.ws.rs.WebApplicationException if user account is disabled
     * @throws javax.ws.rs.WebApplicationException with status 423 (Locked) if login attempts >  ALLOWED_FAILED_LOGIN_ATTEMPTS
     */
    @Transactional
    public Optional<User> authenticate(String username, String password) {
        Optional<UserEntity> userEntityOptional = userDao.findByUsername(username);
        logger.debug("Login attempt - username={}", username);
        if (userEntityOptional.isPresent()) { //interestingly java cannot map/orElseGet this block properly, without getting the compiler confused. :)
            UserEntity userEntity = userEntityOptional.get();
            if (passwordHasher.isEqual(password, userEntity.getPassword())) {
                if (!userEntity.isDisabled()) {
                    userEntity.setLoginCounter(0);
                    userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                    userDao.merge(userEntity);
                }

                logger.info("Successful Login - user_id={}", userEntity.getExternalId());
                return Optional.of(linksBuilder.decorate(userEntity.toUser()));
            } else {
                userEntity.setLoginCounter(userEntity.getLoginCounter() + 1);
                userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                userEntity.setDisabled(userEntity.getLoginCounter() >= loginAttemptCap);
                logger.info("Failed login attempt - user_id={}, login_counter={}", userEntity.getExternalId(), userEntity.getLoginCounter());
                userDao.merge(userEntity);
                if (userEntity.isDisabled()) {
                    logger.warn("Account locked due to exceeding {} attempts - user_id={}", loginAttemptCap, userEntity.getExternalId());
                }
                return Optional.empty();
            }
        } else {
            logger.info("Failed login attempt - user_id='Not matched'");
            return Optional.empty();
        }
    }

    /**
     * finds a user by externalId
     *
     * @param externalId
     * @return {@link User} as an {@link Optional} if found. Otherwise Optional.empty() will be returned.
     */
    public Optional<User> findUserByExternalId(String externalId) {
        Optional<UserEntity> userEntityOptional = userDao.findByExternalId(externalId);
        return userEntityOptional
                .map(userEntity -> Optional.of(
                        linksBuilder.decorate(userEntity.toUser())))
                .orElse(Optional.empty());
    }

    /**
     * finds a user by username
     *
     * @param username
     * @return {@link User} as an {@link Optional} if found. Otherwise Optional.empty() will be returned.
     */
    public Optional<User> findUserByUsername(String username) {
        Optional<UserEntity> userEntityOptional = userDao.findByUsername(username);
        return userEntityOptional
                .map(userEntity -> Optional.of(
                        linksBuilder.decorate(userEntity.toUser())))
                .orElse(Optional.empty());
    }

    public Optional<SecondFactorToken> newSecondFactorPasscode(String externalId) {
        return userDao.findByExternalId(externalId)
                .map(userEntity -> {
                    int newPassCode = secondFactorAuthenticator.newPassCode(userEntity.getOtpKey());
                    SecondFactorToken token = SecondFactorToken.from(externalId, newPassCode);
                    final String userExternalId = userEntity.getExternalId();
                    notificationService.sendSecondFactorPasscodeSms(userEntity.getTelephoneNumber(), token.getPasscode())
                            .thenAcceptAsync(notificationId -> logger.info("sent 2FA token successfully to user [{}], notification id [{}]",
                                    userExternalId, notificationId))
                            .exceptionally(exception -> {
                                logger.error(format("error sending 2FA token to user [%s]", userExternalId), exception);
                                return null;
                            });
                    logger.info("New 2FA token generated for User [{}]", userExternalId);
                    return Optional.of(token);
                })
                .orElseGet(() -> {
                    //this cannot happen unless a bug in selfservice
                    logger.error("New 2FA token attempted for non-existent User [{}]", externalId);
                    return Optional.empty();
                });
    }

    @Transactional
    public Optional<User> authenticateSecondFactor(String externalId, int code) {
        logger.debug("OTP attempt - user_id={}", externalId);
        return userDao.findByExternalId(externalId)
                .map(userEntity -> {
                    if (userEntity.isDisabled()) {
                        logger.warn("Failed OTP attempt - user_id={}, login_counter={}. Authenticate Second Factor attempted for a disabled User", userEntity.getExternalId(), userEntity.getLoginCounter());
                        return Optional.<User>empty();
                    }
                    if (secondFactorAuthenticator.authorize(userEntity.getOtpKey(), code)) {
                        userEntity.setLoginCounter(0);
                        userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                        userDao.merge(userEntity);
                        logger.info("Successful OTP. user_id={}", userEntity.getExternalId());
                        return Optional.of(linksBuilder.decorate(userEntity.toUser()));
                    } else {
                        userEntity.setLoginCounter(userEntity.getLoginCounter() + 1);
                        userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                        userEntity.setDisabled(userEntity.getLoginCounter() > loginAttemptCap);
                        userDao.merge(userEntity);
                        if (userEntity.isDisabled()) {
                            logger.warn("Failed OTP attempt - user_id={}, login_counter={}. Invalid second factor in an account currently locked", userEntity.getExternalId(), userEntity.getLoginCounter());
                        } else {
                            logger.info("Failed OTP attempt - user_id={}, login_counter={}. Invalid second factor attempt.", userEntity.getExternalId(), userEntity.getLoginCounter());
                        }
                        return Optional.<User>empty();
                    }
                })
                .orElseGet(() -> {
                    //this cannot happen unless a bug in selfservice
                    logger.error("Authenticate 2FA token attempted for non-existent User [{}]", externalId);
                    return Optional.empty();
                });
    }

    @Transactional
    public Optional<User> patchUser(String externalId, PatchRequest patchRequest) {

        Optional<UserEntity> userOptional = userDao.findByExternalId(externalId);

        if (!userOptional.isPresent()) {
            return Optional.empty();
        }

        UserEntity user = userOptional.get();

        if (PATH_SESSION_VERSION.equals(patchRequest.getPath())) {
            incrementSessionVersion(user, parseInt(patchRequest.getValue()));
        } else if (PATH_DISABLED.equals(patchRequest.getPath())) {
            changeUserDisabled(user, parseBoolean(patchRequest.getValue()));
        } else if (PATH_TELEPHONE_NUMBER.equals(patchRequest.getPath())) {
            changeUserTelephoneNumber(user, patchRequest.getValue());
        } else {
            String error = format("Invalid patch request with path [%s]", patchRequest.getPath());
            logger.error(error);
            throw new RuntimeException(error);
        }

        return Optional.of(linksBuilder.decorate(user.toUser()));
    }

    private void changeUserTelephoneNumber(UserEntity userEntity, String telephoneNumber) {
        userEntity.setTelephoneNumber(telephoneNumber);
        userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
        userDao.merge(userEntity);
    }

    private void changeUserDisabled(UserEntity userEntity, Boolean value) {
        userEntity.setLoginCounter(0);
        userEntity.setDisabled(value);
        userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
        userDao.merge(userEntity);
    }

    private void incrementSessionVersion(UserEntity userEntity, Integer value) {
        userEntity.setSessionVersion(userEntity.getSessionVersion() + value);
        userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
        userDao.merge(userEntity);
    }

}
