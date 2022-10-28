package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;

import java.util.Locale;
import java.util.Optional;

import static java.lang.String.format;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE;

public class UserOtpDispatcher extends InviteOtpDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserOtpDispatcher.class);
    private final InviteDao inviteDao;
    private final SecondFactorAuthenticator secondFactorAuthenticator;
    private final PasswordHasher passwordHasher;
    private final NotificationService notificationService;

    @Inject
    public UserOtpDispatcher(InviteDao inviteDao, SecondFactorAuthenticator secondFactorAuthenticator,
                             PasswordHasher passwordHasher,
                             NotificationService notificationService) {
        super();
        this.inviteDao = inviteDao;
        this.secondFactorAuthenticator = secondFactorAuthenticator;
        this.passwordHasher = passwordHasher;
        this.notificationService = notificationService;
    }

    @Transactional
    @Override
    public void dispatchOtp(String inviteCode) {
        InviteEntity inviteEntity = inviteDao.findByCode(inviteCode).orElseThrow(AdminUsersExceptions::notFoundException);

        inviteEntity.setTelephoneNumber(TelephoneNumberUtility.formatToE164(inviteOtpRequest.getTelephoneNumber()));
        inviteEntity.setPassword(passwordHasher.hash(inviteOtpRequest.getPassword()));
        inviteDao.merge(inviteEntity);
        int newPassCode = secondFactorAuthenticator.newPassCode(inviteEntity.getOtpKey());
        String passcode = format(Locale.ENGLISH, SIX_DIGITS_WITH_LEADING_ZEROS, newPassCode);
        LOGGER.info("New 2FA token generated for invite code [{}]", inviteCode);

        try {
            String notificationId = notificationService.sendSecondFactorPasscodeSms(inviteOtpRequest.getTelephoneNumber(), passcode,
                    CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE);
            LOGGER.info("sent 2FA token successfully for invite code [{}], notification id [{}]", inviteCode, notificationId);
        } catch (Exception e) {
            LOGGER.info(format("error sending 2FA token for invite code [%s]", inviteCode), e);
        }
    }
}
