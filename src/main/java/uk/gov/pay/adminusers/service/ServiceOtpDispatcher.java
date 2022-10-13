package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.exception.UserNotificationException;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;

import java.util.Locale;
import java.util.Optional;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE;

public class ServiceOtpDispatcher extends InviteOtpDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceOtpDispatcher.class);
    private final InviteDao inviteDao;
    private final SecondFactorAuthenticator secondFactorAuthenticator;
    private final PasswordHasher passwordHasher;
    private final NotificationService notificationService;

    @Inject
    public ServiceOtpDispatcher(InviteDao inviteDao, SecondFactorAuthenticator secondFactorAuthenticator,
                                PasswordHasher passwordHasher, NotificationService notificationService) {
        super();
        this.inviteDao = inviteDao;
        this.secondFactorAuthenticator = secondFactorAuthenticator;
        this.passwordHasher = passwordHasher;
        this.notificationService = notificationService;
    }

    @Override
    public boolean dispatchOtp(String inviteCode) {
        return inviteDao.findByCode(inviteCode)
                .map(inviteEntity -> {
                    Optional.ofNullable(inviteOtpRequest.getTelephoneNumber())
                            .map(TelephoneNumberUtility::formatToE164)
                            .ifPresent(inviteEntity::setTelephoneNumber);

                    Optional.ofNullable(inviteOtpRequest.getPassword())
                            .map(passwordHasher::hash)
                            .ifPresent(inviteEntity::setPassword);

                    inviteDao.merge(inviteEntity);

                    int newPassCode = secondFactorAuthenticator.newPassCode(inviteEntity.getOtpKey());
                    String passcode = format(Locale.ENGLISH, SIX_DIGITS_WITH_LEADING_ZEROS, newPassCode);

                    LOGGER.info("New 2FA token generated for invite code [{}]", inviteEntity.getCode());
                    
                    try {
                        String notificationId = notificationService.sendSecondFactorPasscodeSms(inviteEntity.getTelephoneNumber(), passcode,
                                SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE);
                        LOGGER.info("sent 2FA token successfully for invite code [{}], notification id [{}]", inviteEntity.getCode(), notificationId);
                    } catch (UserNotificationException e) {
                        LOGGER.error(format("error sending 2FA token for invite code [%s]", inviteEntity.getCode()), e);
                    }
                    
                    return true;
                }).orElseGet(() -> {
                    LOGGER.error("Unable to locate invite after validating and reaching to the service otp dispatcher. invite code [{}]", inviteCode);
                    return false;
                });
    }
}
