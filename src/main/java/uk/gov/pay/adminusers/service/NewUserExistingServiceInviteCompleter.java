package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteCompleteResponse;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingEmail;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.inviteLockedException;
import static uk.gov.service.payments.logging.LoggingKeys.SERVICE_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.USER_EXTERNAL_ID;

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
    public InviteCompleteResponse complete(InviteEntity inviteEntity) {
        if (inviteEntity.isExpired() || Boolean.TRUE.equals(inviteEntity.isDisabled())) {
            throw inviteLockedException(inviteEntity.getCode());
        }
        if (userDao.findByEmail(inviteEntity.getEmail()).isPresent()) {
            throw conflictingEmail(inviteEntity.getEmail());
        }
        if (!inviteEntity.isUserType()) {
            throw internalServerError(format("Attempting to complete a 'new user, existing service' invite for an non user invite. invite-code = %s", inviteEntity.getCode()));
        }

        UserEntity userEntity = inviteEntity.mapToUserEntity();
        userDao.persist(userEntity);
        inviteEntity.setDisabled(Boolean.TRUE);
        inviteDao.merge(inviteEntity);

        String serviceIds = userEntity.getServicesRoles().stream()
                .map(serviceRole -> serviceRole.getService().getExternalId())
                .collect(Collectors.joining(", "));

        LOGGER.info(
                Markers.append(USER_EXTERNAL_ID, userEntity.getExternalId())
                        .and(Markers.append(SERVICE_EXTERNAL_ID, serviceIds)),
                "User created successfully from invitation"
        );

        Invite invite = linksBuilder.addUserLink(userEntity.toUser(), inviteEntity.toInvite());
        InviteCompleteResponse response = new InviteCompleteResponse(invite);
        response.setUserExternalId(userEntity.getExternalId());

        return response;
    }

}
