package uk.gov.pay.adminusers.service;

import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.InviteOtpRequest;
import uk.gov.pay.adminusers.model.InviteValidateOtpRequest;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.util.Locale;
import java.util.Optional;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.invalidOtpAuthCodeInviteException;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.inviteLockedException;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.notFoundInviteException;

public class InviteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InviteService.class);

    private static final String SIX_DIGITS_WITH_LEADING_ZEROS = "%06d";

    private final UserDao userDao;
    private final InviteDao inviteDao;
    private final NotificationService notificationService;
    private final SecondFactorAuthenticator secondFactorAuthenticator;
    private final LinksBuilder linksBuilder;

    private final Integer loginAttemptCap;

    @Inject
    public InviteService(UserDao userDao,
                         InviteDao inviteDao,
                         NotificationService notificationService,
                         SecondFactorAuthenticator secondFactorAuthenticator,
                         LinksBuilder linksBuilder,
                         @Named("LOGIN_ATTEMPT_CAP") Integer loginAttemptCap) {
        this.userDao = userDao;
        this.inviteDao = inviteDao;
        this.notificationService = notificationService;
        this.secondFactorAuthenticator = secondFactorAuthenticator;
        this.linksBuilder = linksBuilder;
        this.loginAttemptCap = loginAttemptCap;
    }

    @Deprecated
    @Transactional
    // Refactor to adopt UserOtpDispatcher. And Avoid using generic InviteOtpRequest object to avoid having to use optional fields
    public void reGenerateOtp(InviteOtpRequest inviteOtpRequest) {
        Optional<InviteEntity> inviteOptional = inviteDao.findByCode(inviteOtpRequest.getCode());
        if (inviteOptional.isPresent()) {
            InviteEntity invite = inviteOptional.get();
            invite.setTelephoneNumber(TelephoneNumberUtility.formatToE164(inviteOtpRequest.getTelephoneNumber()));
            inviteDao.merge(invite);
            int newPassCode = secondFactorAuthenticator.newPassCode(invite.getOtpKey());
            String passcode = String.format(Locale.ENGLISH, SIX_DIGITS_WITH_LEADING_ZEROS, newPassCode);
            
            LOGGER.info("New 2FA token generated for invite code [{}]", inviteOtpRequest.getCode());
            
            try {
                String notificationId = notificationService.sendSecondFactorPasscodeSms(inviteOtpRequest.getTelephoneNumber(), passcode);
                LOGGER.info("sent 2FA token successfully for invite code [{}], notification id [{}]", inviteOtpRequest.getCode(), notificationId);
            } catch (Exception e) {
                LOGGER.error(format("error sending 2FA token for invite code [%s]", inviteOtpRequest.getCode()), e);
            }
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

}
