package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.CreateUserRequest;
import uk.gov.pay.adminusers.model.PatchRequest;
import uk.gov.pay.adminusers.model.SecondFactorToken;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_DISABLED;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_SESSION_VERSION;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingServiceGatewayAccountsForUser;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.undefinedRoleException;

public class UserServices {

    private static Logger logger = PayLoggerFactory.getLogger(UserServices.class);

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final ServiceDao serviceDao;
    private final PasswordHasher passwordHasher;
    private final LinksBuilder linksBuilder;
    private final Integer loginAttemptCap;
    private final NotificationService notificationService;
    private final SecondFactorAuthenticator secondFactorAuthenticator;

    @Inject
    public UserServices(UserDao userDao, RoleDao roleDao,
                        ServiceDao serviceDao,
                        PasswordHasher passwordHasher,
                        LinksBuilder linksBuilder,
                        @Named("LOGIN_ATTEMPT_CAP") Integer loginAttemptCap,
                        Provider<NotificationService> userNotificationService, SecondFactorAuthenticator secondFactorAuthenticator) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.serviceDao = serviceDao;
        this.passwordHasher = passwordHasher;
        this.linksBuilder = linksBuilder;
        this.loginAttemptCap = loginAttemptCap;
        this.notificationService = userNotificationService.get();
        this.secondFactorAuthenticator = secondFactorAuthenticator;
    }

    /**
     * persists a new user
     *
     * @param createUserRequest
     * @param roleName          initial role to be assigned
     * @return {@link User} with associated links
     * @throws javax.ws.rs.WebApplicationException with status 409-Conflict if the username is already taken
     * @throws javax.ws.rs.WebApplicationException with status 500 for any unknown error during persistence
     */
    @Transactional
    public User createUser(CreateUserRequest createUserRequest, String roleName) {
        return roleDao.findByRoleName(roleName)
                .map(roleEntity -> {
                    UserEntity userEntity = UserEntity.from(createUserRequest);
                    userEntity.setPassword(passwordHasher.hash(createUserRequest.getPassword()));
                    if (createUserRequest.getServiceIds().isEmpty()) {
                        addServiceRoleToUser(userEntity, roleEntity, createUserRequest.getGatewayAccountIds());
                    } else {
                        addServiceRoleToUser(userEntity, roleEntity, createUserRequest.getServiceIds().get(0));
                    }
                    userDao.persist(userEntity);
                    return linksBuilder.decorate(userEntity.toUser());
                })
                .orElseThrow(() -> undefinedRoleException(roleName));
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
        if (PATH_SESSION_VERSION.equals(patchRequest.getPath())) {
            return incrementSessionVersion(externalId, parseInt(patchRequest.getValue()));
        } else if (PATH_DISABLED.equals(patchRequest.getPath())) {
            return changeUserDisabled(externalId, parseBoolean(patchRequest.getValue()));
        } else {
            String error = format("Invalid patch request with path [%s]", patchRequest.getPath());
            logger.error(error);
            throw new RuntimeException(error);
        }
    }

    private Optional<User> changeUserDisabled(String externalId, Boolean value) {
        return userDao.findByExternalId(externalId)
                .map(userEntity -> {
                    userEntity.setLoginCounter(0);
                    userEntity.setDisabled(value);
                    userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                    userDao.merge(userEntity);
                    return Optional.of(linksBuilder.decorate(userEntity.toUser()));
                })
                .orElseGet(Optional::empty);
    }

    private Optional<User> incrementSessionVersion(String externalId, Integer value) {
        return userDao.findByExternalId(externalId)
                .map(userEntity -> {
                    userEntity.setSessionVersion(userEntity.getSessionVersion() + value);
                    userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                    userDao.merge(userEntity);
                    return Optional.of(linksBuilder.decorate(userEntity.toUser()));
                })
                .orElseGet(Optional::empty);
    }

    private void addServiceRoleToUser(UserEntity user, RoleEntity role, List<String> gatewayAccountIds) {
        ServiceRoleEntity serviceRole = getServiceAssignedTo(gatewayAccountIds)
                .map(serviceEntity -> new ServiceRoleEntity(serviceEntity, role))
                .orElseGet(() -> {
                    ServiceEntity service = new ServiceEntity(gatewayAccountIds);
                    serviceDao.persist(service);
                    return new ServiceRoleEntity(service, role);
                });
        serviceRole.setUser(user);
        user.addServiceRole(serviceRole);
    }

    private void addServiceRoleToUser(UserEntity user, RoleEntity role, String serviceId) {
        ServiceRoleEntity serviceRole = serviceDao.findById(Integer.parseInt(serviceId))
                .map(serviceEntity -> new ServiceRoleEntity(serviceEntity, role))
                .orElseGet(() -> {
                    throw AdminUsersExceptions.serviceDoesNotExistError(serviceId);
                });
        serviceRole.setUser(user);
        user.addServiceRole(serviceRole);
    }

    private Optional<ServiceEntity> getServiceAssignedTo(List<String> gatewayAccountIds) {
        for (String gatewayAccountId : gatewayAccountIds) {
            Optional<ServiceEntity> serviceOptional = serviceDao.findByGatewayAccountId(gatewayAccountId);
            if (serviceOptional.isPresent()) {
                if (serviceOptional.get().hasExactGatewayAccountIds(gatewayAccountIds)) {
                    return serviceOptional;
                } else {
                    throw conflictingServiceGatewayAccountsForUser();
                }
            }
        }
        return Optional.empty();
    }
}
