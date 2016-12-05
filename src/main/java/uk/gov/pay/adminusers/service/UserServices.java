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

import static java.util.Arrays.asList;
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
     * @param validatedUserRequest
     * @param roleName             initial role to be assigned
     * @return {@link User} with associated links
     * @throws javax.ws.rs.WebApplicationException with status 409-Conflict if the username is already taken
     * @throws javax.ws.rs.WebApplicationException with status 500 for any unknown error during persistence
     */
    public User createUser(User validatedUserRequest, String roleName) {
        return roleDao.findByRoleName(roleName)
                .map(roleEntity -> {
                    UserEntity userEntity = UserEntity.from(validatedUserRequest);
                    userEntity.setRoles(asList(roleEntity));
                    userEntity.setPassword(passwordHasher.hash(validatedUserRequest.getPassword()));

                    try {
                        userDao.persist(userEntity);
                        User user = userEntity.toUser();
                        user.setLinks(asList(linksBuilder.buildSelf(user)));
                        return user;
                    } catch (Exception ex) {
                        if (ex.getMessage().contains(CONSTRAINT_VIOLATION_MESSAGE)) {
                            throw conflictingUsername(validatedUserRequest.getUsername());
                        } else {
                            logger.error("unknown database error during user creation for data {}", validatedUserRequest, ex);
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
                .map(userEntity -> {
                    User user = userEntity.toUser();
                    user.setLinks(ImmutableList.of(linksBuilder.buildSelf(user)));
                    return Optional.of(user);
                })
                .orElse(Optional.empty());
    }
}
