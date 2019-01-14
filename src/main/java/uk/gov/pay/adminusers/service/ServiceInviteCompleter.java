package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteCompleteResponse;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.Optional;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.*;

public class ServiceInviteCompleter extends InviteCompleter {


    @Inject
    private final InviteDao inviteDao;
    private final UserDao userDao;
    private final ServiceDao serviceDao;
    private final LinksBuilder linksBuilder;

    @Inject
    public ServiceInviteCompleter(InviteDao inviteDao, UserDao userDao, ServiceDao serviceDao, LinksBuilder linksBuilder) {
        this.inviteDao = inviteDao;
        this.userDao = userDao;
        this.serviceDao = serviceDao;
        this.linksBuilder = linksBuilder;
    }

    /**
     * Completes a service invite.
     * ie. it creates and persists a user from an invite or/and subscribe a user to an existing service
     * and if it is a service invite also creates a default service.
     * It then disables the invite.
     */
    @Override
    @Transactional
    public Optional<InviteCompleteResponse> complete(String inviteCode) {
        return inviteDao.findByCode(inviteCode)
                .map(inviteEntity -> {
                    if (inviteEntity.isExpired() || inviteEntity.isDisabled()) {
                        throw inviteLockedException(inviteEntity.getCode());
                    }
                    if (userDao.findByEmail(inviteEntity.getEmail()).isPresent()) {
                        throw conflictingEmail(inviteEntity.getEmail());
                    }

                    if (inviteEntity.isServiceType()) {
                        UserEntity userEntity = inviteEntity.mapToUserEntity();
                        ServiceEntity serviceEntity = ServiceEntity.from(Service.from());
                        if (!data.getGatewayAccountIds().isEmpty()) {
                            serviceEntity.addGatewayAccountIds(data.getGatewayAccountIds().toArray(new String[0]));
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
                        return Optional.of(response);
                    } else {
                        throw internalServerError(format("Attempting to complete a service invite for a non service invite of type. invite-code = %s", inviteEntity.getCode()));
                    }
                }).orElseGet(Optional::empty);
    }

}
