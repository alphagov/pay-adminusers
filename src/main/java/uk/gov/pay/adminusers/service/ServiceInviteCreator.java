package uk.gov.pay.adminusers.service;

import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.LinksConfig;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteServiceRequest;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.InviteType.SERVICE;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingEmail;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;

public class ServiceInviteCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInviteCreator.class);
    private final InviteDao inviteDao;
    private final UserDao userDao;
    private final RoleDao roleDao;
    private final LinksBuilder linksBuilder;
    private final LinksConfig linksConfig;
    private final NotificationService notificationService;
    private final PasswordHasher passwordHasher;

    @Inject
    public ServiceInviteCreator(InviteDao inviteDao, UserDao userDao, RoleDao roleDao, LinksBuilder linksBuilder,
                                LinksConfig linksConfig, NotificationService notificationService, PasswordHasher passwordHasher) {
        this.inviteDao = inviteDao;
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.linksBuilder = linksBuilder;
        this.linksConfig = linksConfig;
        this.notificationService = notificationService;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public Invite doInvite(InviteServiceRequest inviteServiceRequest) {
        String requestEmail = inviteServiceRequest.getEmail();
        Optional<UserEntity> anExistingUser = userDao.findByEmail(requestEmail);
        if (anExistingUser.isPresent()) {
            UserEntity user = anExistingUser.get();
            if (user.isDisabled()) {
                sendUserDisabledNotification(requestEmail, user.getExternalId());
            } else {
                sendUserExistsNotification(requestEmail, user.getExternalId());
            }
            throw conflictingEmail(requestEmail);
        }

        List<InviteEntity> exitingInvites = inviteDao.findByEmail(requestEmail);
        List<InviteEntity> existingValidServiceInvitesForSameEmail =  exitingInvites.stream()
                .filter(inviteEntity -> !inviteEntity.isDisabled() && !inviteEntity.isExpired())
                .filter(inviteEntity -> SERVICE.getType().equals(inviteEntity.getType())).collect(Collectors.toList());

        if(!existingValidServiceInvitesForSameEmail.isEmpty()) {
            InviteEntity foundInvite = existingValidServiceInvitesForSameEmail.get(0);
            return constructInviteAndSendEmail(inviteServiceRequest, foundInvite, inviteEntity -> {
                inviteDao.merge(inviteEntity);
                return null;
            });
        }

        return roleDao.findByRoleName(inviteServiceRequest.getRoleName())
                .map(roleEntity -> {
                    InviteEntity inviteEntity = new InviteEntity(requestEmail, randomUuid(), inviteServiceRequest.getOtpKey(), roleEntity);
                    inviteEntity.setType(SERVICE);
                    return constructInviteAndSendEmail(inviteServiceRequest, inviteEntity, inviteToPersist -> {
                        inviteDao.persist(inviteToPersist);
                        return null;
                    });
                })
                .orElseThrow(() -> internalServerError(format("Role [%s] not a valid role for creating a invite service request", inviteServiceRequest.getRoleName())));

    }

    private Invite constructInviteAndSendEmail(InviteServiceRequest inviteServiceRequest, InviteEntity inviteEntity, Function<InviteEntity, Void> saveOrUpdate) {
        String inviteUrl = format("%s/%s", linksConfig.getSelfserviceInvitesUrl(), inviteEntity.getCode());
        inviteEntity.setTelephoneNumber(TelephoneNumberUtility.formatToE164(inviteServiceRequest.getTelephoneNumber()));
        inviteEntity.setPassword(passwordHasher.hash(inviteServiceRequest.getPassword()));
        saveOrUpdate.apply(inviteEntity);
        sendServiceInviteNotification(inviteEntity, inviteUrl);
        Invite invite = inviteEntity.toInvite();
        invite.setInviteLink(inviteUrl);
        return linksBuilder.decorate(invite);
    }

    private void sendServiceInviteNotification(InviteEntity invite, String targetUrl) {
        LOGGER.info("New service creation invitation created");
        try {
            String notificationId = notificationService.sendServiceInviteEmail(invite.getEmail(), targetUrl);
            LOGGER.info("sent create service invitation email successfully, notification id [{}]", notificationId);
        } catch(Exception e) {
            LOGGER.error("error sending create service invitation", e);
        }
    }

    private void sendUserDisabledNotification(String email, String userExternalId) {
        LOGGER.info("Disabled existing user tried to create a service - user_id={}", userExternalId);
        try {
            String notificationId = notificationService.sendServiceInviteUserDisabledEmail(email, linksConfig.getSupportUrl());
            LOGGER.info("sent create service, user account disabled email successfully, notification id [{}]", notificationId);
        } catch (Exception e) {
            LOGGER.error("error sending service creation, user account disabled email", e);
        }
    }

    private void sendUserExistsNotification(String email, String userExternalId) {
        LOGGER.info("Existing user tried to create a service - user_id={}", userExternalId);
        try {
            String notificationId = notificationService.sendServiceInviteUserExistsEmail(email, linksConfig.getSelfserviceLoginUrl(), linksConfig.getSelfserviceForgottenPasswordUrl(), linksConfig.getSupportUrl());
            LOGGER.info("sent create service, user exists email successfully, notification id [{}]", notificationId);
        } catch (Exception e) {
            LOGGER.error("error sending service creation, users exists email", e);
        }
    }
}
