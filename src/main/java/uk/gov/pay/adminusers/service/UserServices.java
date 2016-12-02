package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import static java.util.Arrays.asList;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.*;

public class UserServices {

    static final String CONSTRAINT_VIOLATION_MESSAGE = "ERROR: duplicate key value violates unique constraint";
    private static Logger logger = LoggerFactory.getLogger(UserServices.class);

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final PasswordHasher passwordHasher;

    @Inject
    public UserServices(UserDao userDao, RoleDao roleDao, PasswordHasher passwordHasher) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.passwordHasher = passwordHasher;
    }

    public User createUser(User validatedUserRequest, String roleName) {
        return roleDao.findByRoleName(roleName)
                .map(roleEntity -> {
                    UserEntity userEntity = UserEntity.from(validatedUserRequest);
                    userEntity.setRoles(asList(roleEntity));
                    userEntity.setPassword(passwordHasher.hash(validatedUserRequest.getPassword()));

                    try {
                        userDao.persist(userEntity);
                        return userEntity.toUser();
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
}
