package uk.gov.pay.adminusers.service;

import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteOtpRequest;
import uk.gov.pay.adminusers.model.InviteUserRequest;
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
import javax.ws.rs.WebApplicationException;
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
        return inviteDao.findByCode(inviteValidateOtpRequest.getCode())
                .map(inviteEntity -> validateOtp(inviteEntity, inviteValidateOtpRequest.getOtpCode())
                            .map(ValidateOtpAndCreateUserResult::new)
                            .orElseGet(() -> {
                                inviteEntity.setLoginCounter(0);
                                UserEntity userEntity = inviteEntity.mapToUserEntity();
                                userDao.persist(userEntity);
                                inviteEntity.setDisabled(Boolean.TRUE);
                                inviteDao.merge(inviteEntity);
                                return new ValidateOtpAndCreateUserResult(linksBuilder.decorate(userEntity.toUser()));
                }))
                .orElseGet(() -> new ValidateOtpAndCreateUserResult(notFoundInviteException(inviteValidateOtpRequest.getCode())));
    }

    @Transactional
    public Optional<WebApplicationException> validateOtp(InviteValidateOtpRequest inviteOtpRequest) {
        return  inviteDao.findByCode(inviteOtpRequest.getCode())
                .map(inviteEntity -> validateOtp(inviteEntity, inviteOtpRequest.getOtpCode()))
                .orElseGet(() -> Optional.of(notFoundInviteException(inviteOtpRequest.getCode())));
    }

    Optional<WebApplicationException> validateOtp(InviteEntity inviteEntity, int otpCode) {
        if (inviteEntity.isDisabled()) {
            return Optional.of(inviteLockedException(inviteEntity.getCode()));
        }

        if (!secondFactorAuthenticator.authorize(inviteEntity.getOtpKey(), otpCode)) {
            inviteEntity.setLoginCounter(inviteEntity.getLoginCounter() + 1);
            inviteEntity.setDisabled(inviteEntity.getLoginCounter() >= loginAttemptCap);
            inviteDao.merge(inviteEntity);

            if (inviteEntity.isDisabled()) {
                return Optional.of(inviteLockedException(inviteEntity.getCode()));
            }
            return Optional.of(invalidOtpAuthCodeInviteException(inviteEntity.getCode()));
        }
        return Optional.empty();
    }

    private Function<RoleEntity, Optional<Invite>> doInvite(InviteUserRequest invite, ServiceEntity serviceEntity) {
        return role -> {
            Optional<UserEntity> userSender = userDao.findByExternalId(invite.getSender());
            if (userSender.isPresent() && userSender.get().canInviteUsersTo(serviceEntity.getId())) {
                InviteEntity inviteEntity = new InviteEntity(invite.getEmail(), randomUuid(), invite.getOtpKey(), role);
                inviteEntity.setSender(userSender.get());
                inviteEntity.setService(serviceEntity);
                inviteDao.persist(inviteEntity);
                String inviteUrl = fromUri(selfserviceBaseUrl).path(SELFSERVICE_INVITES_PATH).path(inviteEntity.getCode()).build().toString();
                sendInviteNotification(inviteEntity, inviteUrl);
                return Optional.of(inviteEntity.toInvite(inviteUrl));
            } else {
                throw forbiddenOperationException(invite.getSender(), "invite", serviceEntity.getExternalId());
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
}
