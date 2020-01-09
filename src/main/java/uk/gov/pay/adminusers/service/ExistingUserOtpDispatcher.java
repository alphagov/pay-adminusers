package uk.gov.pay.adminusers.service;

import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.SecondFactorToken;
import uk.gov.pay.adminusers.persistence.dao.UserDao;

import javax.inject.Inject;
import java.util.Optional;

import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.LEGACY;

public class ExistingUserOtpDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServices.class);

    private final NotificationService notificationService;
    private final SecondFactorAuthenticator secondFactorAuthenticator;
    private final UserDao userDao;

    @Inject
    public ExistingUserOtpDispatcher(Provider<NotificationService> notificationService, SecondFactorAuthenticator secondFactorAuthenticator,
                                     UserDao userDao) {
        this.notificationService = notificationService.get();
        this.secondFactorAuthenticator = secondFactorAuthenticator;
        this.userDao = userDao;
    }

    public Optional<SecondFactorToken> newSecondFactorPasscode(String externalId, boolean useProvisionalOtpKey) {
        return userDao.findByExternalId(externalId)
                .map(userEntity -> {
                    String otpKeyOrProvisionalOtpKey = useProvisionalOtpKey ? userEntity.getProvisionalOtpKey() : userEntity.getOtpKey();
                    return Optional.ofNullable(otpKeyOrProvisionalOtpKey).map(otpKey -> {
                        int newPassCode = secondFactorAuthenticator.newPassCode(otpKey);
                        SecondFactorToken token = SecondFactorToken.from(externalId, newPassCode);
                        final String userExternalId = userEntity.getExternalId();

                        try {
                            String notificationId = notificationService.sendSecondFactorPasscodeSms(userEntity.getTelephoneNumber(), token.getPasscode(),
                                    LEGACY);
                            LOGGER.info("sent 2FA token successfully to user [{}], notification id [{}]", userExternalId, notificationId);
                        } catch (Exception e) {
                            LOGGER.error("error sending 2FA token to user [{}]", userExternalId, e);
                        }

                        if (useProvisionalOtpKey) {
                            LOGGER.info("New 2FA token generated for User [{}] from provisional OTP key", userExternalId);
                        } else {
                            LOGGER.info("New 2FA token generated for User [{}]", userExternalId);
                        }
                        return Optional.of(token);
                    }).orElseGet(() -> {
                        if (useProvisionalOtpKey) {
                            LOGGER.error("New provisional 2FA token attempted for user without a provisional OTP key [{}]", externalId);
                        } else {
                            // Realistically, this will never happen
                            LOGGER.error("New 2FA token attempted for user without an OTP key [{}]", externalId);
                        }
                        return Optional.empty();
                    });
                })
                .orElseGet(() -> {
                    //this cannot happen unless a bug in selfservice
                    LOGGER.error("New 2FA token attempted for non-existent User [{}]", externalId);
                    return Optional.empty();
                });
    }

}
