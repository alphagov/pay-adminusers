package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;

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
                .orElseGet(Optional::empty);
    }


    public List<Invite> findAllActiveInvites(String serviceId) {
        return inviteDao.findAllByServiceId(serviceId)
                .stream()
                .filter(inviteEntity -> !(inviteEntity.isDisabled()))
                .filter(inviteEntity -> !(inviteEntity.isExpired()))
                .map(inviteEntity -> {
                    Invite invite = inviteEntity.toInvite();
                    return userDao.findByEmail(inviteEntity.getEmail()).map(userEntity -> {
                        invite.setUserExist(true);
                        return invite;
                    }).orElse(invite);
                })
                .collect(Collectors.toList());
    }
}
