package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteCompleteResponse;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingEmail;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.inviteLockedException;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.notFoundInviteException;

public class NewUserExistingServiceInviteCompleter extends InviteCompleter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewUserExistingServiceInviteCompleter.class);
    
    private final InviteDao inviteDao;
    private final UserDao userDao;
    private final LinksBuilder linksBuilder;

    @Inject
    public NewUserExistingServiceInviteCompleter(InviteDao inviteDao, UserDao userDao, LinksBuilder linksBuilder) {
        super();
        this.inviteDao = inviteDao;
        this.userDao = userDao;
        this.linksBuilder = linksBuilder;
    }

    @Override
    @Transactional
    public InviteCompleteResponse complete(String inviteCode) {
        return inviteDao.findByCode(inviteCode)
                .map(inviteEntity -> {
                    if (inviteEntity.isExpired() || inviteEntity.isDisabled()) {
                        throw inviteLockedException(inviteEntity.getCode());
                    }
                    if (userDao.findByEmail(inviteEntity.getEmail()).isPresent()) {
                        throw conflictingEmail(inviteEntity.getEmail());
                    }
                    if (inviteEntity.getType() != InviteType.NEW_USER_INVITED_TO_EXISTING_SERVICE) {
                        throw internalServerError(format("Attempting to complete a 'new user, existing service' invite for an invite of type '%s'", inviteEntity.getCode()));
                    }
                    
                    UserEntity userEntity = inviteEntity.mapToUserEntity();
                    userDao.persist(userEntity);
                    inviteEntity.setDisabled(Boolean.TRUE);
                    inviteDao.merge(inviteEntity);
                    
                    String serviceIds = userEntity.getServicesRoles().stream()
                            .map(serviceRole -> serviceRole.getService().getExternalId())
                            .collect(Collectors.joining(", "));

                    LOGGER.info("User created successfully from invitation [{}] for services [{}]", userEntity.getExternalId(), serviceIds);

                    Invite invite = linksBuilder.addUserLink(userEntity.toUser(), inviteEntity.toInvite());
                    InviteCompleteResponse response = new InviteCompleteResponse(invite);
                    response.setUserExternalId(userEntity.getExternalId());

                    return response;
                })
                .orElseThrow(() -> notFoundInviteException(inviteCode));
    }

}
