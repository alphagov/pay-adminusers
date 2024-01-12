package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.persistence.entity.UserRoleEntity;

import java.util.Optional;

import static uk.gov.pay.adminusers.service.AdminUsersExceptions.undefinedRoleException;

public class UserRolesService {

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final LinksBuilder linksBuilder;

    @Inject
    public UserRolesService(UserDao userDao, RoleDao roleDao, LinksBuilder linksBuilder) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.linksBuilder = linksBuilder;
    }

    @Transactional
    public Optional<User> doUpdate(String userExternalId, String roleName) {
        Optional<UserEntity> userMaybe = userDao.findByExternalId(userExternalId);
        if (!userMaybe.isPresent()) {
            return Optional.empty();
        }
        UserEntity userEntity = userMaybe.get();

        Optional<RoleEntity> roleMaybe = roleDao.findByRoleName(roleName);
        if (!roleMaybe.isPresent()) {
            throw undefinedRoleException(roleName);
        }
        RoleEntity targetRoleEntity = roleMaybe.get();

        if (userEntity.getUserRoles() != null && !userEntity.getUserRoles().isEmpty()) {
            UserRoleEntity userRoleEntity = userEntity.getUserRoles().get(0);

            if (userRoleEntity.getRole().getId() != targetRoleEntity.getId()) {
                userRoleEntity.setRole(targetRoleEntity);
            }
        } else {
            UserRoleEntity userRoleEntity = new UserRoleEntity(targetRoleEntity);
            userEntity.addUserRole(userRoleEntity);
        }
        userDao.persist(userEntity);
        return Optional.of(linksBuilder.decorate(userEntity.toUser()));
    }

    @Transactional
    public Optional<User> removeRole(String userExternalId) {
        Optional<UserEntity> userMaybe = userDao.findByExternalId(userExternalId);
        if (!userMaybe.isPresent()) {
            return Optional.empty();
        }
        UserEntity userEntity = userMaybe.get();

        if (userEntity.getUserRoles() != null) {
            userEntity.removeUserRole();
            userDao.merge(userEntity);
        }

        return Optional.of(linksBuilder.decorate(userEntity.toUser()));
    }
}
