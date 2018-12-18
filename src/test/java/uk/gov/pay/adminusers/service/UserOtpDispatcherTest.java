package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.InviteOtpRequest;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserOtpDispatcherTest {

    @Mock
    InviteDao inviteDao;
    @Mock
    SecondFactorAuthenticator secondFactorAuthenticator;
    @Mock
    NotificationService notificationService;

    final ArgumentCaptor<InviteEntity> expectedInvite = ArgumentCaptor.forClass(InviteEntity.class);

    InviteOtpDispatcher userOtpDispatcher;

    @Before
    public void before() {
        userOtpDispatcher = new UserOtpDispatcher(inviteDao, secondFactorAuthenticator, new PasswordHasher(), notificationService);
    }

    @Test
    public void shouldSuccess_whenDispatchUserOtp_ifInviteEntityExist() {
        String inviteCode = "valid-invite-code";
        String telephone = "78562835762";
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setCode(inviteCode);
        inviteEntity.setType(InviteType.USER);
        inviteEntity.setOtpKey("otp-key");

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("telephone_number", telephone, "password", "random"));
        userOtpDispatcher = userOtpDispatcher.withData(InviteOtpRequest.from(payload));

        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(secondFactorAuthenticator.newPassCode("otp-key")).thenReturn(123456);
        when(notificationService.sendSecondFactorPasscodeSms(telephone, "123456")).thenReturn(CompletableFuture.completedFuture("success code from notify"));
        boolean dispatched = userOtpDispatcher.dispatchOtp(inviteCode);

        verify(inviteDao).merge(expectedInvite.capture());
        assertThat(dispatched, is(true));
        assertThat(expectedInvite.getValue().getTelephoneNumber(),is(telephone));
        assertThat(expectedInvite.getValue().getPassword(),is(notNullValue()));
    }

    @Test
    public void shouldFail_whenDispatchServiceOtp_ifInviteEntityNotFound() {
        String inviteCode = "non-existent-code";
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.empty());

        boolean dispatched = userOtpDispatcher.dispatchOtp(inviteCode);

        assertThat(dispatched,is(false));
    }
}
