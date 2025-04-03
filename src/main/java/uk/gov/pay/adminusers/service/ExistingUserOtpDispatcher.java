package uk.gov.pay.adminusers.service;

import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.SecondFactorToken;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.persistence.entity.UserMfaMethodEntity;

import jakarta.inject.Inject;

import java.util.Optional;

import static uk.gov.pay.adminusers.model.SecondFactorMethod.SMS;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.CHANGE_SIGN_IN_2FA_TO_SMS;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.SIGN_IN;

public class ExistingUserOtpDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingUserOtpDispatcher.class);

    private final NotificationService notificationService;
    private final SecondFactorAuthenticator secondFactorAuthenticator;

    @Inject
    public ExistingUserOtpDispatcher(Provider<NotificationService> notificationService, SecondFactorAuthenticator secondFactorAuthenticator) {
        this.notificationService = notificationService.get();
        this.secondFactorAuthenticator = secondFactorAuthenticator;
    }

    public SecondFactorToken sendSignInOtp(UserEntity userEntity) {
        return sendOtp(userEntity, false);
    }

    public SecondFactorToken sendChangeSignMethodToSmsOtp(UserEntity userEntity) {
        return sendOtp(userEntity, true);
    }

    private SecondFactorToken sendOtp(UserEntity userEntity, boolean changingSignInMethodToSms) {
        String otpKeyOrProvisionalOtpKey;
        
        if(changingSignInMethodToSms){
            otpKeyOrProvisionalOtpKey = userEntity.getProvisionalOtpKey();
        } else {
            Optional<UserMfaMethodEntity> smsMfa = userEntity.getUserMfas()
                    .stream().filter(userMfaMethodEntity -> userMfaMethodEntity.getMethod().equals(SMS))
                    .findFirst();
            if (smsMfa.isPresent()) {
                otpKeyOrProvisionalOtpKey = smsMfa.get().getOtpKey();
            } else {
                otpKeyOrProvisionalOtpKey = "";
            }
        }

        if (otpKeyOrProvisionalOtpKey == null) {
            if (changingSignInMethodToSms) {
                LOGGER.error("New provisional 2FA token attempted for user without a provisional OTP key [{}]", userEntity.getExternalId());
            } else {
                // Realistically, this will never happen
                LOGGER.error("New 2FA token attempted for user without an OTP key [{}]", userEntity.getExternalId());
            }
            throw AdminUsersExceptions.otpKeyMissingException(userEntity.getExternalId());
        }

        return userEntity.getTelephoneNumber().map(telephoneNumber -> {
            int newPassCode = secondFactorAuthenticator.newPassCode(otpKeyOrProvisionalOtpKey);
            SecondFactorToken token = SecondFactorToken.from(userEntity.getExternalId(), newPassCode);
            String userExternalId = userEntity.getExternalId();
            NotificationService.OtpNotifySmsTemplateId notifyTemplateId = changingSignInMethodToSms ? CHANGE_SIGN_IN_2FA_TO_SMS : SIGN_IN;

            try {
                String notificationId = notificationService.sendSecondFactorPasscodeSms(telephoneNumber, token.getPasscode(),
                        notifyTemplateId);
                LOGGER.info("sent 2FA token successfully to user [{}], notification id [{}]", userExternalId, notificationId);
            } catch (Exception e) {
                LOGGER.error("error sending 2FA token to user [{}]", userExternalId, e);
            }


            if (changingSignInMethodToSms) {
                LOGGER.info("New 2FA token generated for User [{}] from provisional OTP key", userExternalId);
            } else {
                LOGGER.info("New 2FA token generated for User [{}]", userExternalId);
            }
            return token;
        }).orElseThrow(() -> {
            throw AdminUsersExceptions.userDoesNotHaveTelephoneNumberError(userEntity.getExternalId());
        });
    }
    
}
