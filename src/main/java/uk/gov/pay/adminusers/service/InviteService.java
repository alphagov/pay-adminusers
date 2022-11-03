package uk.gov.pay.adminusers.service;

import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.model.InviteValidateOtpRequest;
import uk.gov.pay.adminusers.model.ResendOtpRequest;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.util.Locale;
import java.util.Optional;

import static uk.gov.pay.adminusers.service.AdminUsersExceptions.invalidOtpAuthCodeInviteException;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.inviteLockedException;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.notFoundInviteException;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE;

public class InviteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InviteService.class);

    private static final String SIX_DIGITS_WITH_LEADING_ZEROS = "%06d";

    private final InviteDao inviteDao;
    private final NotificationService notificationService;
    private final SecondFactorAuthenticator secondFactorAuthenticator;

    private final Integer loginAttemptCap;

    @Inject
    public InviteService(InviteDao inviteDao,
                         NotificationService notificationService,
                         SecondFactorAuthenticator secondFactorAuthenticator,
                         @Named("LOGIN_ATTEMPT_CAP") Integer loginAttemptCap) {
        this.inviteDao = inviteDao;
        this.notificationService = notificationService;
        this.secondFactorAuthenticator = secondFactorAuthenticator;
        this.loginAttemptCap = loginAttemptCap;
    }
    
    @Transactional
    public void reGenerateOtp(ResendOtpRequest resendOtpRequest) {
        Optional<InviteEntity> inviteOptional = inviteDao.findByCode(resendOtpRequest.getCode());
        if (inviteOptional.isPresent()) {
            InviteEntity invite = inviteOptional.get();
            invite.setTelephoneNumber(TelephoneNumberUtility.formatToE164(resendOtpRequest.getTelephoneNumber()));
            inviteDao.merge(invite);
            int newPassCode = secondFactorAuthenticator.newPassCode(invite.getOtpKey());
            String passcode = String.format(Locale.ENGLISH, SIX_DIGITS_WITH_LEADING_ZEROS, newPassCode);

            LOGGER.info("New 2FA token generated for invite code [{}]", resendOtpRequest.getCode());

            try {
                String notificationId = notificationService.sendSecondFactorPasscodeSms(resendOtpRequest.getTelephoneNumber(), passcode,
                        mapInviteTypeToOtpNotifySmsTemplateId(invite.getType()));
                LOGGER.info("sent 2FA token successfully for invite code [{}], notification id [{}]", resendOtpRequest.getCode(), notificationId);
            } catch (Exception e) {
                LOGGER.error(String.format("error sending 2FA token for invite code [%s]", resendOtpRequest.getCode()), e);
            }
        } else {
            throw notFoundInviteException(resendOtpRequest.getCode());
        }
    }

    @Transactional(ignore = {WebApplicationException.class})
    public void validateOtp(InviteValidateOtpRequest inviteOtpRequest) {
        InviteEntity inviteEntity = inviteDao.findByCode(inviteOtpRequest.getCode()).orElseThrow(() -> notFoundInviteException(inviteOtpRequest.getCode()));
        validateOtp(inviteEntity, inviteOtpRequest.getOtp());
    }
    
    /* default */ void validateOtp(InviteEntity inviteEntity, int otpCode) {
        if (inviteEntity.isDisabled()) {
            throw inviteLockedException(inviteEntity.getCode());
        }

        if (!secondFactorAuthenticator.authorize(inviteEntity.getOtpKey(), otpCode)) {
            inviteEntity.setLoginCounter(inviteEntity.getLoginCounter() + 1);
            inviteEntity.setDisabled(inviteEntity.getLoginCounter() >= loginAttemptCap);
            inviteDao.merge(inviteEntity);

            if (inviteEntity.isDisabled()) {
                throw inviteLockedException(inviteEntity.getCode());
            }
            throw invalidOtpAuthCodeInviteException(inviteEntity.getCode());
        }
    }

    public Optional<InviteEntity> findInvite(String code) {
        return inviteDao.findByCode(code);
    }

    private static OtpNotifySmsTemplateId mapInviteTypeToOtpNotifySmsTemplateId(InviteType inviteType) {
        switch (inviteType) {
            case SERVICE:
                return SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE;
            case USER:
                return CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE;
            default:
                throw new IllegalArgumentException("Unrecognised InviteType: " + inviteType.name());
        }
    }

}
