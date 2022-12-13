package uk.gov.pay.adminusers.service;

import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.CompleteInviteResponse;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.model.InviteValidateOtpRequest;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchOp;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static uk.gov.pay.adminusers.resources.InviteRequestValidator.FIELD_PASSWORD;
import static uk.gov.pay.adminusers.resources.InviteRequestValidator.FIELD_TELEPHONE_NUMBER;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.invalidOtpAuthCodeInviteException;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.inviteDoesNotHaveTelephoneNumberError;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.inviteLockedException;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.missingSecondFactorMethod;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.notFoundInviteException;
import static uk.gov.pay.adminusers.service.AdminUsersExceptions.userAlreadyExistsForSelfRegistration;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE;

public class InviteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InviteService.class);

    private static final String SIX_DIGITS_WITH_LEADING_ZEROS = "%06d";

    private final InviteDao inviteDao;
    private final UserDao userDao;
    private final NotificationService notificationService;
    private final SecondFactorAuthenticator secondFactorAuthenticator;
    private final PasswordHasher passwordHasher;
    private final LinksBuilder linksBuilder;
    private final Integer loginAttemptCap;

    @Inject
    public InviteService(InviteDao inviteDao,
                         UserDao userDao, 
                         NotificationService notificationService,
                         SecondFactorAuthenticator secondFactorAuthenticator,
                         PasswordHasher passwordHasher,
                         LinksBuilder linksBuilder, 
                         @Named("LOGIN_ATTEMPT_CAP") Integer loginAttemptCap) {
        this.inviteDao = inviteDao;
        this.userDao = userDao;
        this.notificationService = notificationService;
        this.secondFactorAuthenticator = secondFactorAuthenticator;
        this.passwordHasher = passwordHasher;
        this.linksBuilder = linksBuilder;
        this.loginAttemptCap = loginAttemptCap;
    }

    @Transactional
    public void sendOtp(String inviteCode) {
        InviteEntity invite = findInvite(inviteCode).orElseThrow(() -> notFoundInviteException(inviteCode));

        if (invite.getTelephoneNumber() == null) {
            throw inviteDoesNotHaveTelephoneNumberError(inviteCode);
        }

        int passCode = secondFactorAuthenticator.newPassCode(invite.getOtpKey());
        String formattedPasscode = String.format(Locale.ENGLISH, SIX_DIGITS_WITH_LEADING_ZEROS, passCode);

        OtpNotifySmsTemplateId templateId = invite.getService().isPresent() ? CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE : SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE;
        try {
            String notificationId = notificationService.sendSecondFactorPasscodeSms(invite.getTelephoneNumber(), formattedPasscode, templateId);
            LOGGER.info("sent 2FA token successfully for invite code [{}], notification id [{}]", invite.getCode(), notificationId);
        } catch (Exception e) {
            LOGGER.error(String.format("error sending 2FA token for invite code [%s]", invite.getCode()), e);
        }
    }
    
    @Transactional
    public Invite reprovisionOtp(String inviteCode) {
        InviteEntity inviteEntity = findInvite(inviteCode).orElseThrow(() -> notFoundInviteException(inviteCode));

        String newOtpKey = secondFactorAuthenticator.generateNewBase32EncodedSecret();
        inviteEntity.setOtpKey(newOtpKey);
        inviteDao.merge(inviteEntity);
        
        return inviteEntity.toInvite();
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

    @Transactional
    public Invite updateInvite(String inviteCode, List<JsonPatchRequest> updateRequests) {
        InviteEntity inviteEntity = inviteDao.findByCode(inviteCode).orElseThrow(() -> notFoundInviteException(inviteCode));
        updateRequests.forEach(updateRequest -> {
            if (JsonPatchOp.REPLACE == updateRequest.getOp()) {
                switch (updateRequest.getPath()) {
                    case FIELD_PASSWORD:
                        inviteEntity.setPassword(passwordHasher.hash(updateRequest.valueAsString()));
                        break;
                    case FIELD_TELEPHONE_NUMBER:
                        inviteEntity.setTelephoneNumber(updateRequest.valueAsString());
                        break;
                    default:
                        throw AdminUsersExceptions.unexpectedPathForPatchOperation(updateRequest.getPath());
                }
            }
        });
        return inviteEntity.toInvite();
    }
        
    @Transactional
    public CompleteInviteResponse complete(String code, @Nullable SecondFactorMethod secondFactorMethod) {
        InviteEntity inviteEntity = inviteDao.findByCode(code).orElseThrow(() -> notFoundInviteException(code));
        if (inviteEntity.isExpired() || Boolean.TRUE.equals(inviteEntity.isDisabled())) {
            throw inviteLockedException(inviteEntity.getCode());
        }

        inviteEntity.setDisabled(true);

        UserEntity userEntity = userDao.findByEmail(inviteEntity.getEmail())
                .map(existingUser -> addExistingUserToService(code, inviteEntity, existingUser))
                .orElseGet(() -> createUserFromInvite(secondFactorMethod, inviteEntity));

        Invite invite = linksBuilder.addUserLink(userEntity.toUser(), inviteEntity.toInvite());
        CompleteInviteResponse response = new CompleteInviteResponse(invite, userEntity.getExternalId());
        inviteEntity.getService().ifPresent(serviceEntity -> response.setServiceExternalId(serviceEntity.getExternalId()));

        return response;
    }

    private UserEntity addExistingUserToService(String code, InviteEntity inviteEntity, UserEntity existingUser) {
        // shouldn't expect the user to exist if there is no service as this indicates the invite is a self-registration
        ServiceEntity serviceEntity = inviteEntity.getService().orElseThrow(() -> userAlreadyExistsForSelfRegistration(inviteEntity.getEmail()));
        
        RoleEntity roleEntity = inviteEntity.getRole().orElseThrow(() -> AdminUsersExceptions.inviteDoesNotHaveRole(code));
        if (existingUser.getServicesRole(serviceEntity.getExternalId()).isEmpty()) {
            ServiceRoleEntity serviceRole = new ServiceRoleEntity(serviceEntity, roleEntity);
            existingUser.addServiceRole(serviceRole);
            userDao.merge(existingUser);
        }
        return existingUser;
    }

    private UserEntity createUserFromInvite(SecondFactorMethod secondFactorMethod, InviteEntity inviteEntity) {
        if (secondFactorMethod == null) {
            throw missingSecondFactorMethod(inviteEntity.getCode());
        }
        UserEntity newUser = inviteEntity.mapToUserEntity(secondFactorMethod);
        userDao.persist(newUser);
        return newUser;
    }

}
