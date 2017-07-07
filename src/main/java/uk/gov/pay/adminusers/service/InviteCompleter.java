package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.Optional;

import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingEmail;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.inviteLockedException;

public class InviteCompleter {
    @Inject
    private final InviteDao inviteDao;
    private final UserDao userDao;
    private final ServiceDao serviceDao;
    private final LinksBuilder linksBuilder;

    @Inject
    public InviteCompleter(InviteDao inviteDao, UserDao userDao, ServiceDao serviceDao,  LinksBuilder linksBuilder) {
        this.inviteDao = inviteDao;
        this.userDao = userDao;
        this.serviceDao = serviceDao;
        this.linksBuilder = linksBuilder;
    }

    /**
     * Completes an invite.
     * ie. it creates and persists a user from an invite
     * and if it is a service invite also creates a default service.
     * It then disables the invite.
     */
    @Transactional
    public Optional<Invite> complete(String inviteCode) {
        return inviteDao.findByCode(inviteCode)
                .map(inviteEntity -> {
                    if (inviteEntity.isExpired() || inviteEntity.isDisabled()) {
                        throw inviteLockedException(inviteEntity.getCode());
                    }

                    if (userDao.findByEmail(inviteEntity.getEmail()).isPresent()) {
                        throw conflictingEmail(inviteEntity.getEmail());
                    }

                    if (inviteEntity.isServiceType()) {
                        ServiceEntity serviceEntity = ServiceEntity.from(Service.from());
                        serviceDao.persist(serviceEntity);
                        inviteEntity.setService(serviceEntity);
                    }

                    UserEntity userEntity = inviteEntity.mapToUserEntity();
                    userDao.persist(userEntity);

                    inviteEntity.setDisabled(true);
                    inviteDao.persist(inviteEntity);

                    return Optional.of(linksBuilder.addUserLink(userEntity.toUser(), inviteEntity.toInvite()));
                }).orElseGet(Optional::empty);
    }
}
