package uk.gov.pay.adminusers.service;

import uk.gov.pay.adminusers.app.util.RandomIdGenerator;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Function;

import static uk.gov.pay.adminusers.service.AdminUsersExceptions.*;

public class InviteService {

    private final RoleDao roleDao;
    private final ServiceDao serviceDao;
    private final UserDao userDao;
    private final InviteDao inviteDao;

    @Inject
    public InviteService(RoleDao roleDao, ServiceDao serviceDao, UserDao userDao, InviteDao inviteDao) {
        this.roleDao = roleDao;
        this.serviceDao = serviceDao;
        this.userDao = userDao;
        this.inviteDao = inviteDao;
    }

    public Optional<Invite> createInvite(int serviceId, String roleName, String email) {

        if (userDao.findByEmail(email).isPresent()) {
            throw conflictingEmail(email);
        }

        return serviceDao.findById(serviceId)
                .flatMap(serviceEntity -> roleDao.findByRoleName(roleName)
                        .map(inviteUser(email, serviceEntity))
                        .orElseThrow(() -> undefinedRoleException(roleName)));
    }

    private Function<RoleEntity, Optional<Invite>> inviteUser(String email, ServiceEntity serviceEntity) {
        return role -> {
            InviteEntity inviteEntity = new InviteEntity(email, RandomIdGenerator.newId(), serviceEntity, role);
            inviteDao.persist(inviteEntity);
            return Optional.of(inviteEntity.toInvite());
        };
    }

    public Optional<Invite> findByCode(String code) {
        return inviteDao.findByCode(code)
                .map(inviteEntity -> {
                    if (inviteEntity.isExpired()) {
                        throw resourceHasExpired();
                    }
                    return Optional.of(inviteEntity.toInvite());
                }).orElseGet(Optional::empty);
    }
}
