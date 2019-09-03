package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;

import java.util.Locale;

import static java.lang.String.format;

public class ServiceOtpDispatcher extends InviteOtpDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceOtpDispatcher.class);
    private final InviteDao inviteDao;
    private final SecondFactorAuthenticator secondFactorAuthenticator;
    private final NotificationService notificationService;

    @Inject
    public ServiceOtpDispatcher(InviteDao inviteDao, SecondFactorAuthenticator secondFactorAuthenticator, NotificationService notificationService) {
        this.inviteDao = inviteDao;
        this.secondFactorAuthenticator = secondFactorAuthenticator;
        this.notificationService = notificationService;
    }

    //This doesn't really need to be transactional. as it read-only from database
    @Override
    public boolean dispatchOtp(String inviteCode) {
        return inviteDao.findByCode(inviteCode)
                .map(inviteEntity -> {
                    int newPassCode = secondFactorAuthenticator.newPassCode(inviteEntity.getOtpKey());
                    String passcode = format(Locale.ENGLISH, SIX_DIGITS_WITH_LEADING_ZEROS, newPassCode);

                    LOGGER.info("New 2FA token generated for invite code [{}]", inviteEntity.getCode());
                    
                    try {
                        String notificationId = notificationService.sendSecondFactorPasscodeSms(inviteEntity.getTelephoneNumber(), passcode);
                        LOGGER.info("sent 2FA token successfully for invite code [{}], notification id [{}]", inviteEntity.getCode(), notificationId);
                    } catch (Exception e) {
                        LOGGER.error(format("error sending 2FA token for invite code [%s]", inviteEntity.getCode()), e);
                    }
                    
                    return true;
                }).orElseGet(() -> {
                    LOGGER.error("Unable to locate invite after validating and reaching to the service otp dispatcher. invite code [{}]", inviteCode);
                    return false;
                });
    }
}
