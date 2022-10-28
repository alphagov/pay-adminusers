package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.CompleteInviteRequest;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteCompleteResponse;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingEmail;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.inviteLockedException;

public class SelfSignupInviteCompleter extends InviteCompleter {

    private final InviteDao inviteDao;
    private final UserDao userDao;
    private final ServiceDao serviceDao;
    private final LinksBuilder linksBuilder;

    @Inject
    public SelfSignupInviteCompleter(InviteDao inviteDao, UserDao userDao, ServiceDao serviceDao, LinksBuilder linksBuilder) {
        super();
        this.inviteDao = inviteDao;
        this.userDao = userDao;
        this.serviceDao = serviceDao;
        this.linksBuilder = linksBuilder;
    }

    /**
     * Completes a self-signup invite.
     * ie. it creates and persists a user from an invite and also creates a default service.
     * It then disables the invite.
     */
    @Override
    @Transactional
    public InviteCompleteResponse complete(InviteEntity inviteEntity, CompleteInviteRequest completeInviteRequest) {
        if (inviteEntity.isExpired() || inviteEntity.isDisabled()) {
            throw inviteLockedException(inviteEntity.getCode());
        }
        if (userDao.findByEmail(inviteEntity.getEmail()).isPresent()) {
            throw conflictingEmail(inviteEntity.getEmail());
        }

        if (inviteEntity.isServiceType()) {
            UserEntity userEntity = inviteEntity.mapToUserEntity();
            ServiceEntity serviceEntity = ServiceEntity.from(Service.from());
            if (completeInviteRequest != null && !completeInviteRequest.getGatewayAccountIds().isEmpty()) {
                serviceEntity.addGatewayAccountIds(completeInviteRequest.getGatewayAccountIds().toArray(new String[0]));
            }
            serviceDao.persist(serviceEntity);

            ServiceRoleEntity serviceRoleEntity = new ServiceRoleEntity(serviceEntity, inviteEntity.getRole());
            userEntity.addServiceRole(serviceRoleEntity);
            userDao.merge(userEntity);

            inviteEntity.setService(serviceEntity);
            inviteEntity.setDisabled(true);
            inviteDao.merge(inviteEntity);

            Invite invite = linksBuilder.addUserLink(userEntity.toUser(), inviteEntity.toInvite());
            InviteCompleteResponse response = new InviteCompleteResponse(invite);
            response.setServiceExternalId(serviceEntity.getExternalId());
            response.setUserExternalId(userEntity.getExternalId());
            return response;
        } else {
            throw internalServerError(format("Attempting to complete a service invite for a non service invite. invite-code = %s", inviteEntity.getCode()));
        }
    }

}
