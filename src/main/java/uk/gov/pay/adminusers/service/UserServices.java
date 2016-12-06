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
