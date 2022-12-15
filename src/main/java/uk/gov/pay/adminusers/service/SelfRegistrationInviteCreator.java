package uk.gov.pay.adminusers.service;

import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.LinksConfig;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.CreateSelfRegistrationInviteRequest;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.stream.Collectors.toUnmodifiableList;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.InviteType.SERVICE;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.conflictingEmail;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.internalServerError;

public class SelfRegistrationInviteCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SelfRegistrationInviteCreator.class);
    
    private static final String ADMIN_ROLE_NAME = "admin";
    
    private final InviteDao inviteDao;
    private final UserDao userDao;
    private final RoleDao roleDao;
    private final LinksBuilder linksBuilder;
    private final LinksConfig linksConfig;
    private final NotificationService notificationService;
    private final SecondFactorAuthenticator secondFactorAuthenticator;

    @Inject
    public SelfRegistrationInviteCreator(InviteDao inviteDao,
                                         UserDao userDao,
                                         RoleDao roleDao,
                                         LinksBuilder linksBuilder,
                                         LinksConfig linksConfig,
                                         NotificationService notificationService,
                                         SecondFactorAuthenticator secondFactorAuthenticator) {
        this.inviteDao = inviteDao;
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.linksBuilder = linksBuilder;
        this.linksConfig = linksConfig;
        this.notificationService = notificationService;
        this.secondFactorAuthenticator = secondFactorAuthenticator;
    }

    @Transactional
    public Invite doInvite(CreateSelfRegistrationInviteRequest createSelfRegistrationInviteRequest) {
        String requestEmail = createSelfRegistrationInviteRequest.getEmail();
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
        List<InviteEntity> existingValidServiceInvitesForSameEmail = exitingInvites.stream()
                .filter(inviteEntity -> !inviteEntity.isDisabled() && !inviteEntity.isExpired())
                .filter(inviteEntity -> !inviteEntity.isInviteToJoinService()).collect(toUnmodifiableList());

        if (!existingValidServiceInvitesForSameEmail.isEmpty()) {
            InviteEntity foundInvite = existingValidServiceInvitesForSameEmail.get(0);
            return constructInviteAndSendEmail(foundInvite, inviteEntity -> {
                inviteDao.merge(inviteEntity);
                return null;
            });
        }

        return roleDao.findByRoleName(ADMIN_ROLE_NAME)
                .map(roleEntity -> {
                    String otpKey = secondFactorAuthenticator.generateNewBase32EncodedSecret();
                    InviteEntity inviteEntity = new InviteEntity(requestEmail, randomUuid(), otpKey, roleEntity);
                    inviteEntity.setType(SERVICE);
                    return constructInviteAndSendEmail(inviteEntity, inviteToPersist -> {
                        inviteDao.persist(inviteToPersist);
                        return null;
                    });
                })
                .orElseThrow(() -> internalServerError("Unable to retrieve admin service role"));

    }

    private Invite constructInviteAndSendEmail(InviteEntity inviteEntity, Function<InviteEntity, Void> saveOrUpdate) {
        String inviteUrl = format("%s/%s", linksConfig.getSelfserviceInvitesUrl(), inviteEntity.getCode());
        saveOrUpdate.apply(inviteEntity);
        sendInviteNotification(inviteEntity, inviteUrl);
        Invite invite = inviteEntity.toInvite();
        invite.setInviteLink(inviteUrl);
        return linksBuilder.decorate(invite);
    }

    private void sendInviteNotification(InviteEntity invite, String targetUrl) {
        LOGGER.info("New self-registration invitation created");
        try {
            String notificationId = notificationService.sendSelfRegistrationInviteEmail(invite.getEmail(), targetUrl);
            LOGGER.info("sent self-registration invitation email successfully, notification id [{}]", notificationId);
        } catch (Exception e) {
            LOGGER.error("error sending self-registration invitation", e);
        }
    }

    private void sendUserDisabledNotification(String email, String userExternalId) {
        LOGGER.info("Disabled existing user tried to initiate self-registration - user_id={}", userExternalId);
        try {
            String notificationId = notificationService.sendSelfRegistrationInviteUserExistsAndIsDisabledEmail(email, linksConfig.getSupportUrl());
            LOGGER.info("sent 'disabled existing user tried initiate self-registration' email successfully, notification id [{}]", notificationId);
        } catch (Exception e) {
            LOGGER.error("error sending 'disabled existing user tried initiate self-registration' email", e);
        }
    }

    private void sendUserExistsNotification(String email, String userExternalId) {
        LOGGER.info("Existing user tried to initiate self-registration - user_id={}", userExternalId);
        try {
            String notificationId = notificationService.sendSelfRegistrationInviteUserExistsEmail(email, linksConfig.getSelfserviceLoginUrl(), linksConfig.getSelfserviceForgottenPasswordUrl(), linksConfig.getSupportUrl());
            LOGGER.info("sent 'existing user tried to initiate self-registration' email successfully, notification id [{}]", notificationId);
        } catch (Exception e) {
            LOGGER.error("error sending 'existing user tried to initiate self-registration' email", e);
        }
    }
}
