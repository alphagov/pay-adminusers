package uk.gov.pay.adminusers.service;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.PatchRequest;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
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
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.*;

public class UserServices {

    static final String CONSTRAINT_VIOLATION_MESSAGE = "ERROR: duplicate key value violates unique constraint";
    private static Logger logger = PayLoggerFactory.getLogger(UserServices.class);

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final PasswordHasher passwordHasher;
    private final LinksBuilder linksBuilder;
    private final Integer loginAttemptCap;

    @Inject
    public UserServices(UserDao userDao, RoleDao roleDao, PasswordHasher passwordHasher, LinksBuilder linksBuilder, @Named("LOGIN_ATTEMPT_CAP") Integer loginAttemptCap) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.passwordHasher = passwordHasher;
        this.linksBuilder = linksBuilder;
        this.loginAttemptCap = loginAttemptCap;
    }

    /**
     * persists a new user
     *
     * @param user
     * @param roleName initial role to be assigned
     * @return {@link User} with associated links
     * @throws javax.ws.rs.WebApplicationException with status 409-Conflict if the username is already taken
     * @throws javax.ws.rs.WebApplicationException with status 500 for any unknown error during persistence
     */
    public User createUser(User user, String roleName) {
        return roleDao.findByRoleName(roleName)
                .map(roleEntity -> {
                    UserEntity userEntity = UserEntity.from(user);
                    userEntity.setRoles(ImmutableList.of(roleEntity));
                    userEntity.setPassword(passwordHasher.hash(user.getPassword()));

                    try {
                        userDao.persist(userEntity);
                        return linksBuilder.decorate(userEntity.toUser());
                    } catch (Exception ex) {
                        if (ex.getMessage().contains(CONSTRAINT_VIOLATION_MESSAGE)) {
                            throw conflictingUsername(user.getUsername());
                        } else {
                            logger.error("unknown database error during user creation for data {}", user, ex);
                            throw internalServerError("unable to create user at this moment");
                        }
                    }
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
    public Optional<User> authenticate(String username, String password) {
        Optional<UserEntity> userEntityOptional = userDao.findByUsername(username);

        if (userEntityOptional.isPresent()) { //interestingly java cannot map/orElseGet this block properly, without getting the compiler confused. :)
            UserEntity userEntity = userEntityOptional.get();
            if (passwordHasher.isEqual(password, userEntity.getPassword())) {
                if (userEntity.isDisabled()) {
                    throw userLockedException(username);
                }
                userEntity.setLoginCount(0);
                userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                userDao.merge(userEntity);
                return Optional.of(linksBuilder.decorate(userEntity.toUser()));
            } else {
                userEntity.setLoginCount(userEntity.getLoginCount() + 1);
                userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                //currently we can only unlock an account by script, manually
                userEntity.setDisabled(userEntity.getLoginCount() > loginAttemptCap);
                userDao.merge(userEntity);
                if (userEntity.isDisabled()) {
                    throw userLockedException(username);
                }
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * finds a user by username
     *
     * @param username
     * @return {@link User} as an {@link Optional} if found. Otherwise Optional.empty() will be returned.
     */
    public Optional<User> findUser(String username) {
        Optional<UserEntity> userEntityOptional = userDao.findByUsername(username);
        return userEntityOptional
                .map(userEntity -> Optional.of(
                        linksBuilder.decorate(userEntity.toUser())))
                .orElse(Optional.empty());
    }


    /**
     * increment login count if a user with given username found
     *
     * @param username
     * @return {@link Optional<User>} if login count less that maximum allowed attempts. Or Optional.empty() if given username not found
     * @throws javax.ws.rs.WebApplicationException if user account is disabled
     */
    public Optional<User> recordLoginAttempt(String username) {
        return userDao.findByUsername(username)
                .map(userEntity -> {
                    userEntity.setLoginCount(userEntity.getLoginCount() + 1);
                    userEntity.setDisabled(userEntity.getLoginCount() > loginAttemptCap);
                    userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                    userDao.merge(userEntity);
                    if (userEntity.isDisabled()) {
                        throw userLockedException(username);
                    }
                    return Optional.of(linksBuilder.decorate(userEntity.toUser()));
                })
                .orElseGet(Optional::empty);
    }

    /**
     * resets users login attempts to 0.
     *
     * @param username
     * @return {@link Optional<User>} if user found and resets to 0. Or Optional.empty() if given username not found
     */
    public Optional<User> resetLoginAttempts(String username) {
        return userDao.findByUsername(username)
                .map(userEntity -> {
                    userEntity.setLoginCount(0);
                    userEntity.setDisabled(false);
                    userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                    userDao.merge(userEntity);
                    return Optional.of(linksBuilder.decorate(userEntity.toUser()));
                })
                .orElseGet(Optional::empty);
    }


    public Optional<User> patchUser(String username, PatchRequest patchRequest) {
        if (PATH_SESSION_VERSION.equals(patchRequest.getPath())) {
            return incrementSessionVersion(username, parseInt(patchRequest.getValue()));
        } else if (PATH_DISABLED.equals(patchRequest.getPath())) {
            return changeUserDisabled(username, parseBoolean(patchRequest.getValue()));
        } else {
            String error = format("Invalid patch request with path [%s]", patchRequest.getPath());
            logger.error(error);
            throw new RuntimeException(error);
        }
    }

    private Optional<User> changeUserDisabled(String username, Boolean value) {
        return userDao.findByUsername(username)
                .map(userEntity -> {
                    userEntity.setDisabled(value);
                    userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                    userDao.merge(userEntity);
                    return Optional.of(linksBuilder.decorate(userEntity.toUser()));
                })
                .orElseGet(Optional::empty);
    }

    private Optional<User> incrementSessionVersion(String username, Integer value) {
        return userDao.findByUsername(username)
                .map(userEntity -> {
                    userEntity.setSessionVersion(userEntity.getSessionVersion() + value);
                    userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                    userDao.merge(userEntity);
                    return Optional.of(linksBuilder.decorate(userEntity.toUser()));
                })
                .orElseGet(Optional::empty);
    }
}
