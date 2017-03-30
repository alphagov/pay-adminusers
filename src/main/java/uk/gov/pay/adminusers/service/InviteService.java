package uk.gov.pay.adminusers.service;

import uk.gov.pay.adminusers.app.util.RandomIdGenerator;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

import javax.inject.Inject;

public class InviteService {

    private final RoleDao roleDao;
    private final InviteDao inviteDao;

    @Inject
    public InviteService(RoleDao roleDao, InviteDao inviteDao) {
        this.roleDao = roleDao;
        this.inviteDao = inviteDao;
    }

    public void create(Invite invite) {
        roleDao.findByRoleName(invite.getRoleName())
                .ifPresent(role -> inviteDao.persist(new InviteEntity(invite.getEmail(), RandomIdGenerator.newId(), role)));

    }
}
