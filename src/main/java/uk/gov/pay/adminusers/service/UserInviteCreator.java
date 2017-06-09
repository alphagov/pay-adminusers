package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.app.config.LinksConfig;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteUserRequest;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.Optional;

import static java.lang.String.format;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.InviteType.USER;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.*;

public class UserInviteCreator {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(UserInviteCreator.class);

    private final InviteDao inviteDao;
    private final UserDao userDao;
    private final RoleDao roleDao;
    private final LinksConfig linksConfig;
    private final NotificationService notificationService;
    private final ServiceDao serviceDao;

    @Inject
    public UserInviteCreator(InviteDao inviteDao, UserDao userDao, RoleDao roleDao, LinksConfig linksConfig, NotificationService notificationService, ServiceDao serviceDao) {
        this.inviteDao = inviteDao;
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.linksConfig = linksConfig;
        this.notificationService = notificationService;
        this.serviceDao = serviceDao;
    }

    @Transactional
    public Optional<Invite> doInvite(InviteUserRequest inviteUserRequest) {

        if (userDao.findByEmail(inviteUserRequest.getEmail()).isPresent()) {
            throw conflictingEmail(inviteUserRequest.getEmail());
        }

        Optional<InviteEntity> inviteOptional = inviteDao.findByEmail(inviteUserRequest.getEmail());
        if (inviteOptional.isPresent()) {
            // When multiple services support is implemented
            // then this should include serviceId
            InviteEntity foundInvite = inviteOptional.get();
            if (Boolean.FALSE.equals(foundInvite.isExpired()) &&
                    Boolean.FALSE.equals(foundInvite.isDisabled())) {
                throw conflictingInvite(inviteUserRequest.getEmail());
            }
        }

        Optional<ServiceEntity> serviceEntityOptional = serviceDao.findByExternalId(inviteUserRequest.getServiceExternalId());
        if(!serviceEntityOptional.isPresent()) {
            return Optional.empty();
        }

        ServiceEntity serviceEntity = serviceEntityOptional.get();
        return roleDao.findByRoleName(inviteUserRequest.getRoleName())
                .map(role -> {
                    Optional<UserEntity> userSender = userDao.findByExternalId(inviteUserRequest.getSender());
                    if (userSender.isPresent() && userSender.get().canInviteUsersTo(serviceEntity.getId())) {
                        InviteEntity inviteEntity = new InviteEntity(inviteUserRequest.getEmail(), randomUuid(), inviteUserRequest.getOtpKey(), userSender.get(), serviceEntity, role);
                        inviteEntity.setType(USER);
                        inviteDao.persist(inviteEntity);
                        String inviteUrl = fromUri(linksConfig.getSelfserviceInvitesUrl()).path(inviteEntity.getCode()).build().toString();
                        sendUserInviteNotification(inviteEntity, inviteUrl);
                        Invite invite = inviteEntity.toInvite();
                        invite.setInviteLink(inviteUrl);
                        return Optional.of(invite);
                    } else {
                        throw forbiddenOperationException(inviteUserRequest.getSender(), "invite", serviceEntity.getExternalId());
                    }
                })
                .orElseThrow(() -> undefinedRoleException(inviteUserRequest.getRoleName()));
    }

    private void sendUserInviteNotification(InviteEntity inviteEntity, String inviteUrl) {
        UserEntity sender = inviteEntity.getSender();
        notificationService.sendInviteEmail(inviteEntity.getSender().getEmail(), inviteEntity.getEmail(), inviteUrl)
                .thenAcceptAsync(notificationId -> LOGGER.info("sent invite email successfully by user [{}], notification id [{}]", sender.getExternalId(), notificationId))
                .exceptionally(exception -> {
                    LOGGER.error(format("error sending email by user [%s]", sender.getExternalId()), exception);
                    return null;
                });
        LOGGER.info("New invite created by User [{}]", sender.getExternalId());
    }
}
