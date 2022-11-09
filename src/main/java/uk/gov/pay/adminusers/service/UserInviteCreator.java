package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.LinksConfig;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteUserRequest;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.stream.Collectors.toUnmodifiableList;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.InviteType.USER;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingInvite;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.forbiddenOperationException;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.undefinedRoleException;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.userAlreadyInService;

public class UserInviteCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserInviteCreator.class);

    private final InviteDao inviteDao;
    private final UserDao userDao;
    private final RoleDao roleDao;
    private final LinksConfig linksConfig;
    private final NotificationService notificationService;
    private final ServiceDao serviceDao;
    private final SecondFactorAuthenticator secondFactorAuthenticator;

    @Inject
    public UserInviteCreator(InviteDao inviteDao,
                             UserDao userDao,
                             RoleDao roleDao,
                             LinksConfig linksConfig,
                             NotificationService notificationService,
                             ServiceDao serviceDao,
                             SecondFactorAuthenticator secondFactorAuthenticator) {
        this.inviteDao = inviteDao;
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.linksConfig = linksConfig;
        this.notificationService = notificationService;
        this.serviceDao = serviceDao;
        this.secondFactorAuthenticator = secondFactorAuthenticator;
    }

    @Transactional
    public Optional<Invite> doInvite(InviteUserRequest inviteUserRequest) {
        Optional<ServiceEntity> serviceEntityOptional = serviceDao.findByExternalId(inviteUserRequest.getServiceExternalId());
        if (serviceEntityOptional.isEmpty()) {
            return Optional.empty();
        }

        Optional<UserEntity> existingUser = userDao.findByEmail(inviteUserRequest.getEmail());
        existingUser.ifPresent(userEntity -> {
            if (userEntity.getServicesRole(inviteUserRequest.getServiceExternalId()).isPresent()) {
                throw userAlreadyInService(userEntity.getExternalId(), inviteUserRequest.getServiceExternalId());
            }
        });

        List<InviteEntity> existingInvites = inviteDao.findByEmail(inviteUserRequest.getEmail());
        List<InviteEntity> validInvitesToTheSameService = existingInvites.stream()
                .filter(inviteEntity -> !inviteEntity.isDisabled() && !inviteEntity.isExpired())
                .filter(inviteEntity -> inviteEntity.getService().isPresent() && inviteUserRequest.getServiceExternalId().equals(inviteEntity.getService().get().getExternalId()))
                .collect(toUnmodifiableList());

        if (!validInvitesToTheSameService.isEmpty()) {
            InviteEntity existingInvite = validInvitesToTheSameService.get(0);
            if (inviteUserRequest.getSender().equals(existingInvite.getSender().getExternalId())) {
                String inviteUrl = fromUri(linksConfig.getSelfserviceInvitesUrl()).path(existingInvite.getCode()).build().toString();
                existingUser.ifPresentOrElse(
                        userEntity -> {
                            ServiceEntity serviceEntity = existingInvite.getService().orElseThrow(() -> AdminUsersExceptions.existingUserInviteDoesNotHaveService(existingInvite.getCode()));
                            sendInviteExistingUserToServiceNotification(existingInvite, inviteUrl, serviceEntity);
                        },
                        () -> sendNewUserInviteNotification(existingInvite, inviteUrl)
                );
                Invite invite = existingInvite.toInvite();
                invite.setInviteLink(inviteUrl);
                return Optional.of(invite);
            } else {
                throw conflictingInvite(inviteUserRequest.getEmail());
            }
        }

        ServiceEntity serviceEntity = serviceEntityOptional.get();

        return roleDao.findByRoleName(inviteUserRequest.getRoleName())
                .map(role -> {
                    Optional<UserEntity> userSender = userDao.findByExternalId(inviteUserRequest.getSender());
                    if (userSender.isPresent() && userSender.get().canInviteUsersTo(serviceEntity.getId())) {
                        String otpKey = secondFactorAuthenticator.generateNewBase32EncodedSecret();
                        InviteEntity inviteEntity = new InviteEntity(inviteUserRequest.getEmail(), randomUuid(), otpKey, role);
                        inviteEntity.setSender(userSender.get());
                        inviteEntity.setService(serviceEntity);
                        inviteEntity.setType(USER);
                        inviteDao.persist(inviteEntity);
                        String inviteUrl = fromUri(linksConfig.getSelfserviceInvitesUrl()).path(inviteEntity.getCode()).build().toString();
                        existingUser.ifPresentOrElse(
                                userEntity -> sendInviteExistingUserToServiceNotification(inviteEntity, inviteUrl, serviceEntity),
                                () -> sendNewUserInviteNotification(inviteEntity, inviteUrl)
                        );
                        Invite invite = inviteEntity.toInvite();
                        invite.setInviteLink(inviteUrl);
                        return Optional.of(invite);
                    } else {
                        throw forbiddenOperationException(inviteUserRequest.getSender(), "invite", serviceEntity.getExternalId());
                    }
                })
                .orElseThrow(() -> undefinedRoleException(inviteUserRequest.getRoleName()));
    }

    private void sendNewUserInviteNotification(InviteEntity inviteEntity, String inviteUrl) {
        UserEntity sender = inviteEntity.getSender();
        LOGGER.info("New invite created by User [{}]", sender.getExternalId());
        try {
            String notificationId = notificationService.sendInviteEmail(inviteEntity.getSender().getEmail(), inviteEntity.getEmail(), inviteUrl);

            LOGGER.info("sent invite email successfully by user [{}], notification id [{}]", sender.getExternalId(), notificationId);
        } catch (Exception e) {
            LOGGER.error(format("error sending email by user [%s]", sender.getExternalId()), e);
        }
    }

    private void sendInviteExistingUserToServiceNotification(InviteEntity inviteEntity, String inviteUrl, ServiceEntity serviceEntity) {
        UserEntity sender = inviteEntity.getSender();
        LOGGER.info("New invite created by User [{}]", sender.getExternalId());
        try {
            String serviceName = serviceEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName();
            String notificationId = notificationService.sendInviteExistingUserEmail(inviteEntity.getSender().getEmail(), inviteEntity.getEmail(), inviteUrl, serviceName);

            LOGGER.info("sent invite email successfully by user [{}], notification id [{}]", sender.getExternalId(), notificationId);
        } catch (Exception e) {
            LOGGER.error(format("error sending email by user [%s]", sender.getExternalId()), e);
        }
    }
}
