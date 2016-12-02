package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import static java.util.Arrays.asList;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.undefinedRoleException;

public class UserServices {
    private final UserDao userDao;
    private final RoleDao roleDao;

    @Inject
    public UserServices(UserDao userDao, RoleDao roleDao) {
        this.userDao = userDao;
        this.roleDao = roleDao;
    }

    public User createUser(User validatedUserRequest, String roleName) {
        return roleDao.findByRoleName(roleName)
                .map(roleEntity -> {
                    UserEntity userEntity = UserEntity.from(validatedUserRequest);
                    userEntity.setRoles(asList(roleEntity));
                    //encrypt password here
                    userDao.persist(userEntity);
                    return userEntity.toUser();
                })
                .orElseThrow(() -> undefinedRoleException(roleName));
    }
}
