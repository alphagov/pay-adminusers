package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;

import java.util.Optional;

import static uk.gov.pay.adminusers.service.AdminUsersExceptions.inviteLockedException;

public class InviteFinder {

    private final InviteDao inviteDao;
    private final UserDao userDao;

    @Inject
    public InviteFinder(InviteDao inviteDao, UserDao userDao) {
        this.inviteDao = inviteDao;
        this.userDao = userDao;
    }

    public Optional<Invite> find(String code) {
        return inviteDao.findByCode(code)
                .map(inviteEntity -> {
                    if (inviteEntity.isExpired() || inviteEntity.isDisabled()) {
                        throw inviteLockedException(inviteEntity.getCode());
                    }
                    Invite invite = inviteEntity.toInvite();
                    return userDao.findByEmail(inviteEntity.getEmail()).map(userEntity -> {
                        invite.setUserExist(true);
                        return Optional.of(invite);
                    }).orElse(Optional.of(invite));
                })
                .orElseGet(() -> Optional.empty());
    }
}
