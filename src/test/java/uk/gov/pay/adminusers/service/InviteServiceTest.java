package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.InviteOtpRequest;
import uk.gov.pay.adminusers.model.ResendOtpRequest;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

import javax.ws.rs.WebApplicationException;
import java.util.Optional;

import static java.lang.String.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_CODE;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_PASSWORD;
import static uk.gov.pay.adminusers.model.InviteOtpRequest.FIELD_TELEPHONE_NUMBER;
import static uk.gov.pay.adminusers.model.InviteType.SERVICE;
import static uk.gov.pay.adminusers.model.InviteType.USER;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    private static final String TELEPHONE_NUMBER = "+441134960000";
    private static final String PLAIN_PASSWORD = "my-secure-pass";

    @Mock
    private InviteDao mockInviteDao;
    @Mock
    private NotificationService mockNotificationService;
    @Mock
    private SecondFactorAuthenticator mockSecondFactorAuthenticator;

    private InviteService inviteService;
    private final ArgumentCaptor<InviteEntity> expectedInvite = ArgumentCaptor.forClass(InviteEntity.class);
    private final int passCode = 123456;
    private final String otpKey = "otpKey";
    private final String inviteCode = "code";

    @BeforeEach
    void setUp() {
        inviteService = new InviteService(
                mockInviteDao,
                mockNotificationService,
                mockSecondFactorAuthenticator,
                3
        );
    }

    @Test
    void generateOtp_shouldSendNotificationOnSuccessfulServiceInviteUpdate() {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setType(SERVICE);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(TELEPHONE_NUMBER), eq(valueOf(passCode)), eq(SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE)))
                .thenReturn("random-notify-id");

        inviteService.reGenerateOtp(new ResendOtpRequest(inviteCode, TELEPHONE_NUMBER));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(TELEPHONE_NUMBER));
    }

    @Test
    void generateOtp_shouldStillUpdateTheServiceInviteWhen2FAFails() {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setType(SERVICE);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(TELEPHONE_NUMBER), eq(valueOf(passCode)), eq(SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE)))
                .thenThrow(AdminUsersExceptions.userNotificationError(new Exception("Cause")));

        inviteService.reGenerateOtp(new ResendOtpRequest(inviteCode, TELEPHONE_NUMBER));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(TELEPHONE_NUMBER));
    }

    @Test
    void generateOtp_shouldSendNotificationOnSuccessfulUserInviteUpdate() {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setType(USER);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(TELEPHONE_NUMBER), eq(valueOf(passCode)),
                eq(CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE))).thenReturn("random-notify-id");

        inviteService.reGenerateOtp(new ResendOtpRequest(inviteCode, TELEPHONE_NUMBER));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(TELEPHONE_NUMBER));
    }

    @Test
    void generateOtp_shouldStillUpdateTheUserInviteWhen2FAFails() {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setType(USER);
        when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(mockSecondFactorAuthenticator.newPassCode(otpKey)).thenReturn(passCode);
        when(mockInviteDao.merge(any(InviteEntity.class))).thenReturn(inviteEntity);
        when(mockNotificationService.sendSecondFactorPasscodeSms(eq(TELEPHONE_NUMBER), eq(valueOf(passCode)),
                eq(CREATE_USER_IN_RESPONSE_TO_INVITATION_TO_SERVICE))).thenThrow(AdminUsersExceptions.userNotificationError(new Exception("Cause")));

        inviteService.reGenerateOtp(new ResendOtpRequest(inviteCode, TELEPHONE_NUMBER));

        verify(mockInviteDao).merge(expectedInvite.capture());
        InviteEntity updatedInvite = expectedInvite.getValue();
        assertThat(updatedInvite.getTelephoneNumber(), is(TELEPHONE_NUMBER));
    }

    @Test
    void validateOtp_shouldNotThrowForValidInviteAndValidOtp() {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);

        when(mockSecondFactorAuthenticator.authorize(otpKey, passCode)).thenReturn(true);

        assertDoesNotThrow(() -> inviteService.validateOtp(inviteEntity, passCode));
    }

    @Test
    void validateOtp_shouldReturnFalseOnValidInviteAndInValidOtp() {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> inviteService.validateOtp(inviteEntity, passCode));
        assertThat(exception.getResponse().getStatus(), is(401));
    }

    @Test
    void validateOtp_shouldReturnFalseOnValidInviteAndValidOtpAndEntityDisabled() {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setOtpKey(otpKey);
        inviteEntity.setDisabled(true);

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> inviteService.validateOtp(inviteEntity, passCode));
        assertThat(exception.getResponse().getStatus(), is(410));
    }

    private InviteOtpRequest inviteOtpRequestFrom(String code, String telephoneNumber, String password) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put(FIELD_CODE, code);
        json.put(FIELD_TELEPHONE_NUMBER, telephoneNumber);
        json.put(FIELD_PASSWORD, password);
        return InviteOtpRequest.from(json);
    }

}
