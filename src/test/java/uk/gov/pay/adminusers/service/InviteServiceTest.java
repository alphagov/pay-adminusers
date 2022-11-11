package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.ResendOtpRequest;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.service.payments.commons.api.exception.ValidationException;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.valueOf;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.fixtures.InviteEntityFixture.anInviteEntity;
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
    @Mock
    private PasswordHasher mockPasswordHasher;

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
                mockPasswordHasher,
                3
        );
    }

    @Nested
    class generateOtp {
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
    }

    @Nested
    class validateOtp {
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
    }
    
    @Nested
    class updateInvite {
        @Test
        void shouldUpdateInvite() {
            InviteEntity inviteEntity = anInviteEntity().withCode(inviteCode).build();
            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));

            var updatePasswordRequest = JsonPatchRequest.from(objectMapper.valueToTree(
                    Map.of("path", "password",
                            "op", "replace",
                            "value", PLAIN_PASSWORD)
            ));
            var updatePhoneNumberRequest = JsonPatchRequest.from(objectMapper.valueToTree(
                    Map.of("path", "telephone_number",
                            "op", "replace",
                            "value", TELEPHONE_NUMBER)
            ));
            
            String hashedPassword = "hashed";
            when(mockPasswordHasher.hash(PLAIN_PASSWORD)).thenReturn(hashedPassword);
            
            inviteService.updateInvite(inviteCode, List.of(updatePasswordRequest, updatePhoneNumberRequest));
            assertThat(inviteEntity.getPassword(), is(hashedPassword));
            assertThat(inviteEntity.getTelephoneNumber(), is(TELEPHONE_NUMBER));
        }

        @Test
        void shouldThrowWhenInviteNotFound() {
            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.empty());

            var updateRequest = JsonPatchRequest.from(objectMapper.valueToTree(
                    Map.of("path", "password",
                            "op", "replace",
                            "value", PLAIN_PASSWORD)
            )); 
            
            var exception = assertThrows(WebApplicationException.class, () -> inviteService.updateInvite(inviteCode, List.of(updateRequest)));
            assertThat(exception.getResponse().getStatus(), is(NOT_FOUND.getStatusCode()));
        }

        @Test
        void shouldThrowWhenPathNotSupported() {
            InviteEntity inviteEntity = anInviteEntity().withCode(inviteCode).build();
            when(mockInviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));

            var updateRequest = JsonPatchRequest.from(objectMapper.valueToTree(
                    Map.of("path", "foo",
                            "op", "replace",
                            "value", "bar")
            ));

            var exception = assertThrows(WebApplicationException.class, () -> inviteService.updateInvite(inviteCode, List.of(updateRequest)));
            assertThat(exception.getResponse().getStatus(), is(BAD_REQUEST.getStatusCode()));
        }
    }

}
