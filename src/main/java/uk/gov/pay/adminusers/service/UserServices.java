package uk.gov.pay.adminusers.service;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.Optional;

import static uk.gov.pay.adminusers.service.AdminUsersExceptions.*;

public class UserServices {

    static final String CONSTRAINT_VIOLATION_MESSAGE = "ERROR: duplicate key value violates unique constraint";
    private static final int ALLOWED_FAILED_LOGIN_ATTEMPTS = 3;
    private static Logger logger = PayLoggerFactory.getLogger(UserServices.class);

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final PasswordHasher passwordHasher;
    private final LinksBuilder linksBuilder;

    @Inject
    public UserServices(UserDao userDao, RoleDao roleDao, PasswordHasher passwordHasher, LinksBuilder linksBuilder) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.passwordHasher = passwordHasher;
        this.linksBuilder = linksBuilder;
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
     * @throws  javax.ws.rs.WebApplicationException with status 423 (Locked) if login attempts >  ALLOWED_FAILED_LOGIN_ATTEMPTS
     */
    public Optional<User> authenticate(String username, String password) {
        Optional<UserEntity> userEntityOptional = userDao.findEnabledUserByUsernameAndPassword(username, passwordHasher.hash(password));

        return userEntityOptional
                .map(userEntity -> {
                    userEntity.setLoginCount(0);
                    userDao.merge(userEntity);
                    return Optional.of(userWithLinks(userEntity));
                })
                .orElseGet(() -> {
                    userDao.findByUsername(username)
                            .ifPresent(userEntity -> {
                                userEntity.setLoginCount(userEntity.getLoginCount() + 1);
                                userEntity.setDisabled(userEntity.getLoginCount() > ALLOWED_FAILED_LOGIN_ATTEMPTS);
                                //TODO how do we enable the user back?
                                userDao.merge(userEntity);
                                if (userEntity.getDisabled()) {
                                    throw userLockedException(username);
                                }
                            });
                    return Optional.empty();
                });

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

}
