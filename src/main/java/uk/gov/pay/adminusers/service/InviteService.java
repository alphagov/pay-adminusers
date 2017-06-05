package uk.gov.pay.adminusers.service;

import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteOtpRequest;
import uk.gov.pay.adminusers.model.InviteRequest;
import uk.gov.pay.adminusers.model.InviteValidateOtpRequest;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.*;

public class InviteService {

    private static final Logger LOGGER = PayLoggerFactory.getLogger(InviteService.class);

    private static final String SELFSERVICE_INVITES_PATH = "invites";
    private static final String SIX_DIGITS_WITH_LEADING_ZEROS = "%06d";

    private final RoleDao roleDao;
    private final ServiceDao serviceDao;
    private final UserDao userDao;
    private final InviteDao inviteDao;
    private final PasswordHasher passwordHasher;
    private final NotificationService notificationService;
    private final SecondFactorAuthenticator secondFactorAuthenticator;
    private final LinksBuilder linksBuilder;
    private final String selfserviceBaseUrl;

    private final Integer loginAttemptCap;

    @Inject
    public InviteService(RoleDao roleDao,
                         ServiceDao serviceDao,
                         UserDao userDao,
                         InviteDao inviteDao,
                         AdminUsersConfig config,
                         PasswordHasher passwordHasher,
                         NotificationService notificationService,
                         SecondFactorAuthenticator secondFactorAuthenticator,
                         LinksBuilder linksBuilder,
                         @Named("LOGIN_ATTEMPT_CAP") Integer loginAttemptCap) {
        this.roleDao = roleDao;
        this.serviceDao = serviceDao;
        this.userDao = userDao;
        this.inviteDao = inviteDao;
        this.passwordHasher = passwordHasher;
        this.notificationService = notificationService;
        this.secondFactorAuthenticator = secondFactorAuthenticator;
        this.selfserviceBaseUrl = config.getLinks().getSelfserviceUrl();
        this.linksBuilder = linksBuilder;
        this.loginAttemptCap = loginAttemptCap;
    }

    @Transactional
    public Optional<Invite> create(InviteRequest invite, int serviceId) {

        if (userDao.findByEmail(invite.getEmail()).isPresent()) {
            throw conflictingEmail(invite.getEmail());
        }

        Optional<InviteEntity> inviteOptional = inviteDao.findByEmail(invite.getEmail());
        if (inviteOptional.isPresent()) {
            // When multiple services support is implemented
            // then this should include serviceId
            InviteEntity foundInvite = inviteOptional.get();
            if (Boolean.FALSE.equals(foundInvite.isExpired()) &&
                    Boolean.FALSE.equals(foundInvite.isDisabled())) {
                throw conflictingInvite(invite.getEmail());
            }
        }

        return serviceDao.findById(serviceId)
                .flatMap(serviceEntity -> roleDao.findByRoleName(invite.getRoleName())
                        .map(doInvite(invite, serviceEntity))
                        .orElseThrow(() -> undefinedRoleException(invite.getRoleName())));
    }

    private Function<RoleEntity, Optional<Invite>> doInvite(InviteRequest invite, ServiceEntity serviceEntity) {
        return role -> {
            Optional<UserEntity> userSender = userDao.findByExternalId(invite.getSender());
            if (userSender.isPresent() && userSender.get().canInviteUsersTo(serviceEntity.getId())) {
                InviteEntity inviteEntity = new InviteEntity(invite.getEmail(), randomUuid(), invite.getOtpKey(), userSender.get(), serviceEntity, role);
                inviteDao.persist(inviteEntity);
                String inviteUrl = fromUri(selfserviceBaseUrl).path(SELFSERVICE_INVITES_PATH).path(inviteEntity.getCode()).build().toString();
                sendInviteNotification(inviteEntity, inviteUrl);
                return Optional.of(inviteEntity.toInvite(inviteUrl));
            } else {
                throw forbiddenOperationException(invite.getSender(), "invite", serviceEntity.getId());
            }
        };
    }

    private void sendInviteNotification(InviteEntity invite, String targetUrl) {
        String senderId = invite.getSender().getExternalId();
        notificationService.sendInviteEmail(invite.getSender().getEmail(), invite.getEmail(), targetUrl)
                .thenAcceptAsync(notificationId -> LOGGER.info("sent invite email successfully by user [{}], notification id [{}]", senderId, notificationId))
                .exceptionally(exception -> {
                    LOGGER.error(format("error sending email by user [%s]", senderId), exception);
                    return null;
                });
        LOGGER.info("New invite created by User [{}]", senderId);
    }

    public Optional<Invite> findByCode(String code) {
        return inviteDao.findByCode(code)
                .map(inviteEntity -> {
                    if (inviteEntity.isExpired() || inviteEntity.isDisabled()) {
                        throw inviteLockedException(inviteEntity.getCode());
                    }
                    return Optional.of(inviteEntity.toInvite());
                }).orElseGet(Optional::empty);
    }

    @Transactional
    public void generateOtp(InviteOtpRequest inviteOtpRequest) {
        Optional<InviteEntity> inviteOptional = inviteDao.findByCode(inviteOtpRequest.getCode());
        if (inviteOptional.isPresent()) {
            InviteEntity invite = inviteOptional.get();
            invite.setTelephoneNumber(inviteOtpRequest.getTelephoneNumber());
            invite.setPassword(passwordHasher.hash(inviteOtpRequest.getPassword()));
            inviteDao.merge(invite);
            int newPassCode = secondFactorAuthenticator.newPassCode(invite.getOtpKey());
            String passcode = String.format(Locale.ENGLISH, SIX_DIGITS_WITH_LEADING_ZEROS, newPassCode);
            notificationService.sendSecondFactorPasscodeSms(inviteOtpRequest.getTelephoneNumber(), passcode)
                    .thenAcceptAsync(notificationId -> LOGGER.info("sent 2FA token successfully for invite code [{}], notification id [{}]",
                            inviteOtpRequest.getCode(), notificationId))
                    .exceptionally(exception -> {
                        LOGGER.error(format("error sending 2FA token for invite code [%s]", inviteOtpRequest.getCode()), exception);
                        return null;
                    });
            LOGGER.info("New 2FA token generated for invite code [{}]", inviteOtpRequest.getCode());
        } else {
            throw notFoundInviteException(inviteOtpRequest.getCode());
        }
    }

    @Transactional
    public ValidateOtpAndCreateUserResult validateOtpAndCreateUser(InviteValidateOtpRequest inviteValidateOtpRequest) {
        Optional<InviteEntity> inviteOptional = inviteDao.findByCode(inviteValidateOtpRequest.getCode());
        if (inviteOptional.isPresent()) {
            InviteEntity inviteEntity = inviteOptional.get();
            if (!secondFactorAuthenticator.authorize(inviteEntity.getOtpKey(), inviteValidateOtpRequest.getOtpCode())) {
                // "failed login attempt" logic
                inviteEntity.setLoginCounter(inviteEntity.getLoginCounter() + 1);
                inviteEntity.setDisabled(inviteEntity.getLoginCounter() >= loginAttemptCap);
                inviteDao.merge(inviteEntity);
                // check if the Invite is locked and fail with GONE / 410 exception if it is
                if (inviteEntity.isDisabled()) {
                    return new ValidateOtpAndCreateUserResult(inviteLockedException(inviteEntity.getCode()));
                }
                // otherwise, fail with UNAUTHORIZED / 401 exception if the otp validation failed
                return new ValidateOtpAndCreateUserResult(invalidOtpAuthCodeInviteException(inviteEntity.getCode()));
            } else {
                // "successful login attempt" logic
                inviteEntity.setLoginCounter(0);
            }
            // check if the Invite is locked and fail with UNAUTHORIZED / 401 exception if it is
            if (inviteEntity.isDisabled()) {
                return new ValidateOtpAndCreateUserResult(inviteLockedException(inviteEntity.getCode()));
            }
            // persist the new User
            UserEntity userEntity = inviteEntity.mapToUserEntity();
            userDao.persist(userEntity);
            // Deactivate the Invite
            inviteEntity.setDisabled(Boolean.TRUE);
            inviteDao.merge(inviteEntity);
            // return the new User with "links" property
            return new ValidateOtpAndCreateUserResult(linksBuilder.decorate(userEntity.toUser()));
        } else {
            return new ValidateOtpAndCreateUserResult(notFoundInviteException(inviteValidateOtpRequest.getCode()));
        }
    }
}
