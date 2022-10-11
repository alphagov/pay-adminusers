package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.InviteCompleteResponse;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.inviteLockedException;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.notFoundInviteException;

public class ExistingUserInviteCompleter extends InviteCompleter {

    private final InviteDao inviteDao;
    private final UserDao userDao;

    @Inject
    public ExistingUserInviteCompleter(InviteDao inviteDao, UserDao userDao) {
        super();
        this.inviteDao = inviteDao;
        this.userDao = userDao;
    }

    @Override
    @Transactional
    public InviteCompleteResponse complete(String inviteCode) {
        return inviteDao.findByCode(inviteCode)
                .map(inviteEntity -> {
                    if (inviteEntity.isExpired() || inviteEntity.isDisabled()) {
                        throw inviteLockedException(inviteEntity.getCode());
                    }
                    return userDao.findByEmail(inviteEntity.getEmail())
                            .map(userEntity -> {
                                if (inviteEntity.getService() != null && inviteEntity.getType().isExistingUserExistingService()) {
                                    ServiceRoleEntity serviceRole = new ServiceRoleEntity(inviteEntity.getService(), inviteEntity.getRole());
                                    userEntity.addServiceRole(serviceRole);
                                    userDao.merge(userEntity);

                                    inviteEntity.setDisabled(true);
                                    inviteDao.merge(inviteEntity);

                                    InviteCompleteResponse response = new InviteCompleteResponse(inviteEntity.toInvite());
                                    response.setUserExternalId(userEntity.getExternalId());
                                    response.setServiceExternalId(inviteEntity.getService().getExternalId());
                                    return response;
                                } else {
                                    throw internalServerError(format("Attempting to complete user subscription to a service for a non existent service. invite-code = %s", inviteEntity.getCode()));
                                }
                            }).orElseThrow(() ->
                                internalServerError(format(
                                        "Attempting to complete user subscription to a service for a non existent user. invite-code = %s",
                                        inviteEntity.getCode()
                                )));
                }).orElseThrow(() -> notFoundInviteException(inviteCode));

    }
}
