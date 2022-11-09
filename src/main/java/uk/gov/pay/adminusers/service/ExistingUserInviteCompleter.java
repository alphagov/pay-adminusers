package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.InviteCompleteResponse;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;

import javax.ws.rs.WebApplicationException;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.inviteLockedException;

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
    public InviteCompleteResponse complete(InviteEntity inviteEntity) {
        if (inviteEntity.isExpired() || Boolean.TRUE.equals(inviteEntity.isDisabled())) {
            throw inviteLockedException(inviteEntity.getCode());
        }
        if (!inviteEntity.isUserType()) {
            throw internalServerError(format("Attempting to complete a user invite for a non user invite. invite-code = %s", inviteEntity.getCode()));
        }
        
        return userDao.findByEmail(inviteEntity.getEmail())
                .map(userEntity -> {
                    ServiceEntity serviceEntity = inviteEntity.getService().orElseThrow(() -> AdminUsersExceptions.existingUserInviteDoesNotHaveService(inviteEntity.getCode()));
                    RoleEntity roleEntity = inviteEntity.getRole().orElseThrow(() -> AdminUsersExceptions.inviteDoesNotHaveRole(inviteEntity.getCode()));
                    ServiceRoleEntity serviceRole = new ServiceRoleEntity(serviceEntity, roleEntity);
                    userEntity.addServiceRole(serviceRole);
                    userDao.merge(userEntity);

                    inviteEntity.setDisabled(true);
                    inviteDao.merge(inviteEntity);

                    InviteCompleteResponse response = new InviteCompleteResponse(inviteEntity.toInvite());
                    response.setUserExternalId(userEntity.getExternalId());
                    response.setServiceExternalId(serviceEntity.getExternalId());
                    return response;
                }).orElseThrow(() ->
                        internalServerError(format(
                                "Attempting to complete user subscription to a service for a non existent user. invite-code = %s",
                                inviteEntity.getCode()
                        )));

    }
}
