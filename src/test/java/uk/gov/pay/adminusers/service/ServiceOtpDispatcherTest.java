package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.InviteOtpRequest;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

import javax.ws.rs.WebApplicationException;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.service.NotificationService.OtpNotifySmsTemplateId.SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE;

@ExtendWith(MockitoExtension.class)
class ServiceOtpDispatcherTest {

    @Mock
    private InviteDao inviteDao;
    @Mock
    private SecondFactorAuthenticator secondFactorAuthenticator;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ArgumentCaptor<InviteEntity> expectedInvite = ArgumentCaptor.forClass(InviteEntity.class);

    private InviteOtpDispatcher serviceOtpDispatcher;

    @BeforeEach
    void before() {
        serviceOtpDispatcher = new ServiceOtpDispatcher(inviteDao, secondFactorAuthenticator, passwordHasher, notificationService);
        serviceOtpDispatcher.withData(InviteOtpRequest.from(objectMapper.valueToTree(Map.of())));
    }

    @Test
    void shouldSuccess_whenDispatchServiceOtp_ifInviteEntityExist() {
        String inviteCode = "valid-invite-code";
        String telephone = "+441134960000";
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setCode(inviteCode);
        inviteEntity.setType(InviteType.SERVICE);
        inviteEntity.setOtpKey("otp-key");
        inviteEntity.setTelephoneNumber(telephone);

        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(secondFactorAuthenticator.newPassCode("otp-key")).thenReturn(123456);
        when(notificationService.sendSecondFactorPasscodeSms(telephone, "123456", SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE))
                .thenReturn("success code from notify");
        assertDoesNotThrow(() -> serviceOtpDispatcher.dispatchOtp(inviteCode));
    }

    @Test
    void shouldSuccess_whenDispatchServiceOtp_ifInviteEntityExist_butPhoneAndPasswordOnlyInRequest_andUpdateInviteEntityWithPhoneAndPassword() {
        String inviteCode = "valid-invite-code";
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setCode(inviteCode);
        inviteEntity.setType(InviteType.SERVICE);
        inviteEntity.setOtpKey("otp-key");

        String telephone = "+447700900000";
        String password = "random"; // pragma: allowlist secret
        serviceOtpDispatcher.withData(InviteOtpRequest.from(objectMapper.valueToTree(Map.of("telephone_number", telephone, "password", "random"))));

        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(passwordHasher.hash(password)).thenReturn("hashed-password");
        when(secondFactorAuthenticator.newPassCode("otp-key")).thenReturn(123456);
        when(notificationService.sendSecondFactorPasscodeSms(telephone, "123456", SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE))
                .thenReturn("success code from notify");
        serviceOtpDispatcher.dispatchOtp(inviteCode);

        verify(inviteDao).merge(expectedInvite.capture());
        assertThat(expectedInvite.getValue().getTelephoneNumber(), is(telephone));
        assertThat(expectedInvite.getValue().getPassword(), is("hashed-password"));
    }

    @Test
    void shouldSuccess_whenDispatchServiceOtp_ifInviteEntityExistWithPassword_butPhoneOnlyInRequest_andUpdateInviteEntityWithPhone() {
        String inviteCode = "valid-invite-code";
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setCode(inviteCode);
        inviteEntity.setType(InviteType.SERVICE);
        inviteEntity.setOtpKey("otp-key");
        inviteEntity.setPassword("hashed-password");

        String telephone = "+447700900000";
        serviceOtpDispatcher.withData(InviteOtpRequest.from(objectMapper.valueToTree(Map.of("telephone_number", telephone))));

        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(secondFactorAuthenticator.newPassCode("otp-key")).thenReturn(123456);
        when(notificationService.sendSecondFactorPasscodeSms(telephone, "123456", SELF_INITIATED_CREATE_NEW_USER_AND_SERVICE))
                .thenReturn("success code from notify");
        serviceOtpDispatcher.dispatchOtp(inviteCode);

        verify(inviteDao).merge(expectedInvite.capture());
        assertThat(expectedInvite.getValue().getTelephoneNumber(), is(telephone));
        assertThat(expectedInvite.getValue().getPassword(), is("hashed-password"));
    }

    @Test
    void shouldFail_whenDispatchServiceOtp_ifInviteEntityNotFound() {

        String inviteCode = "non-existent-code";
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.empty());

        assertThrows(WebApplicationException.class, () -> serviceOtpDispatcher.dispatchOtp(inviteCode));
    }
}
