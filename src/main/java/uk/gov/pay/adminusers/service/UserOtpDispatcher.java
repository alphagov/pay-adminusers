package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;

import java.util.Locale;

import static java.lang.String.format;

public class UserOtpDispatcher extends InviteOtpDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceOtpDispatcher.class);
    private final InviteDao inviteDao;
    private final SecondFactorAuthenticator secondFactorAuthenticator;
    private final PasswordHasher passwordHasher;
    private final NotificationService notificationService;

    @Inject
    public UserOtpDispatcher(InviteDao inviteDao, SecondFactorAuthenticator secondFactorAuthenticator, PasswordHasher passwordHasher, NotificationService notificationService) {
        this.inviteDao = inviteDao;
        this.secondFactorAuthenticator = secondFactorAuthenticator;
        this.passwordHasher = passwordHasher;
        this.notificationService = notificationService;
    }

    @Transactional
    @Override
    public boolean dispatchOtp(String inviteCode) {
        return inviteDao.findByCode(inviteCode)
                .map(inviteEntity -> {
                    inviteEntity.setTelephoneNumber(TelephoneNumberUtility.formatToE164(inviteOtpRequest.getTelephoneNumber()));
                    inviteEntity.setPassword(passwordHasher.hash(inviteOtpRequest.getPassword()));
                    inviteDao.merge(inviteEntity);
                    int newPassCode = secondFactorAuthenticator.newPassCode(inviteEntity.getOtpKey());
                    String passcode = format(Locale.ENGLISH, SIX_DIGITS_WITH_LEADING_ZEROS, newPassCode);
                    notificationService.sendSecondFactorPasscodeSms(inviteOtpRequest.getTelephoneNumber(), passcode)
                            .thenAcceptAsync(notificationId -> LOGGER.info("sent 2FA token successfully for invite code [{}], notification id [{}]",
                                    inviteCode, notificationId))
                            .exceptionally(exception -> {
                                LOGGER.error(format("error sending 2FA token for invite code [%s]", inviteCode), exception);
                                return null;
                            });
                    LOGGER.info("New 2FA token generated for invite code [{}]", inviteCode);
                    return true;
                }).orElseGet(() -> {
                    LOGGER.info("New 2FA token generated for invite code [{}]", inviteCode);
                    return false;
                });
    }
}
