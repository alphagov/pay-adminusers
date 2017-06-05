package uk.gov.pay.adminusers.service;

import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteServiceRequest;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

import javax.inject.Inject;
import java.util.Optional;

import static java.lang.String.format;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.InviteType.SERVICE;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.*;

public class InviteCreator {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(InviteCreator.class);
    private static final String SELFSERVICE_INVITES_PATH = "invites";

    private final InviteDao inviteDao;
    private final UserDao userDao;
    private RoleDao roleDao;
    private final LinksBuilder linksBuilder;
    private String selfserviceBaseUrl;
    private final NotificationService notificationService;

    @Inject
    public InviteCreator(InviteDao inviteDao, UserDao userDao, RoleDao roleDao,LinksBuilder linksBuilder,
                         @Named("SELFSERVICE_BASE_URL") String selfserviceBaseUrl, NotificationService notificationService) {
        this.inviteDao = inviteDao;
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.linksBuilder = linksBuilder;
        this.selfserviceBaseUrl = selfserviceBaseUrl;
        this.notificationService = notificationService;
    }

    @Transactional
    public Invite doCreate(InviteServiceRequest inviteServiceRequest) {
        if (userDao.findByEmail(inviteServiceRequest.getEmail()).isPresent()) {
            throw conflictingEmail(inviteServiceRequest.getEmail());
        }

        Optional<InviteEntity> inviteOptional = inviteDao.findByEmail(inviteServiceRequest.getEmail());
        if (inviteOptional.isPresent()) {
            // When multiple services support is implemented
            // then this should include serviceId
            InviteEntity foundInvite = inviteOptional.get();
            if (!foundInvite.isExpired() && !foundInvite.isDisabled()) {
                throw conflictingInvite(inviteServiceRequest.getEmail());
            }
        }

        return roleDao.findByRoleName(inviteServiceRequest.getRoleName())
                .map(roleEntity -> {
                    InviteEntity inviteEntity = new InviteEntity(inviteServiceRequest.getEmail(), randomUuid(), inviteServiceRequest.getOtpKey(), null, null, roleEntity);
                    inviteEntity.setTelephoneNumber(inviteServiceRequest.getTelephoneNumber());
                    inviteEntity.setType(SERVICE);
                    inviteDao.persist(inviteEntity);
                    String inviteUrl = fromUri(selfserviceBaseUrl).path(SELFSERVICE_INVITES_PATH).path(inviteEntity.getCode()).build().toString();
                    sendInviteNotification(inviteEntity, inviteUrl);
                    return inviteEntity.toInvite(inviteUrl);
                })
                .orElseThrow(()-> internalServerError(format("Role [%s] not a valid role for creating a invite service request",inviteServiceRequest.getRoleName())));

    }

    private void sendInviteNotification(InviteEntity invite, String targetUrl) {
        notificationService.sendServiceInviteEmail(invite.getEmail(), targetUrl)
                .thenAcceptAsync(notificationId -> LOGGER.info("sent create service invitation email successfully, notification id [{}]", notificationId))
                .exceptionally(exception -> {
                    LOGGER.error("error sending create service invitation", exception);
                    return null;
                });
        LOGGER.info("New service creation invitation created");
    }
}
