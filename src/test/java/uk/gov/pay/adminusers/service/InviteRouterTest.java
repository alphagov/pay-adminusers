package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.model.InviteType.EXISTING_USER_INVITED_TO_EXISTING_SERVICE;
import static uk.gov.pay.adminusers.model.InviteType.NEW_USER_AND_NEW_SERVICE_SELF_SIGNUP;
import static uk.gov.pay.adminusers.model.InviteType.NEW_USER_INVITED_TO_EXISTING_SERVICE;
import static uk.gov.pay.adminusers.model.InviteType.SERVICE;
import static uk.gov.pay.adminusers.model.InviteType.USER;

@ExtendWith(MockitoExtension.class)
public class InviteRouterTest {

    @Mock
    private InviteDao inviteDao;

    @Mock
    private InviteServiceFactory inviteServiceFactory;

    private InviteRouter inviteRouter;

    @BeforeEach
    public void before() {
        inviteRouter = new InviteRouter(inviteServiceFactory, inviteDao);
    }

    @Test
    public void shouldResolve_selfSignupInviteCompleter_withValidation() {
        String inviteCode = "a-code";
        InviteEntity inviteEntity = anInvite(SERVICE);
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(inviteServiceFactory.completeSelfSignupInvite()).thenReturn(new SelfSignupInviteCompleter(null, null, null, null));
        Optional<InviteCompleter> result = inviteRouter.routeComplete(inviteCode);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(instanceOf(SelfSignupInviteCompleter.class)));
    }

    @Test
    public void shouldResolve_existingUserInviteCompleter_withoutValidation() {
        String inviteCode = "a-code";
        InviteEntity inviteEntity = anInvite(USER);
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(inviteServiceFactory.completeExistingUserInvite()).thenReturn(new ExistingUserInviteCompleter(null, null));
        Optional<InviteCompleter> result = inviteRouter.routeComplete(inviteCode);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(instanceOf(ExistingUserInviteCompleter.class)));
    }

    @Test
    public void shouldResolve_existingUserInviteDispatcher_forUserInviteType() {
        InviteEntity inviteEntity = anInvite(USER);
        when(inviteServiceFactory.dispatchUserOtp()).thenReturn(new UserOtpDispatcher(null, null, null, null));
        InviteOtpDispatcher otpDispatcher = inviteRouter.routeOtpDispatch(inviteEntity);
        
        assertThat(otpDispatcher, is(instanceOf(UserOtpDispatcher.class)));
    }

    @Test
    public void shouldResolve_selfSignupInviteDispatcher_forServiceInviteType() {
        InviteEntity inviteEntity = anInvite(SERVICE);
        when(inviteServiceFactory.dispatchServiceOtp()).thenReturn(new ServiceOtpDispatcher(null, null, null, null));
        InviteOtpDispatcher otpDispatcher = inviteRouter.routeOtpDispatch(inviteEntity);
        
        assertThat(otpDispatcher, is(instanceOf(ServiceOtpDispatcher.class)));
    }

    @Test
    public void shouldResolve_selfSignupInviteCompleter() {
        String inviteCode = "a-code";
        InviteEntity inviteEntity = anInvite(NEW_USER_AND_NEW_SERVICE_SELF_SIGNUP);
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(inviteServiceFactory.completeSelfSignupInvite()).thenReturn(new SelfSignupInviteCompleter(null, null, null, null));
        Optional<InviteCompleter> result = inviteRouter.routeComplete(inviteCode);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(instanceOf(SelfSignupInviteCompleter.class)));
    }

    @Test
    public void shouldResolve_newUserExistingServiceInviteCompleter() {
        String inviteCode = "a-code";
        InviteEntity inviteEntity = anInvite(NEW_USER_INVITED_TO_EXISTING_SERVICE);
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(inviteServiceFactory.completeNewUserExistingServiceInvite()).thenReturn(new NewUserExistingServiceInviteCompleter(null, null, null));
        Optional<InviteCompleter> result = inviteRouter.routeComplete(inviteCode);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(instanceOf(NewUserExistingServiceInviteCompleter.class)));
    }

    @Test
    public void shouldResolve_existingUserInviteCompleter() {
        String inviteCode = "a-code";
        InviteEntity inviteEntity = anInvite(EXISTING_USER_INVITED_TO_EXISTING_SERVICE);
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(inviteServiceFactory.completeExistingUserInvite()).thenReturn(new ExistingUserInviteCompleter(null, null));
        Optional<InviteCompleter> result = inviteRouter.routeComplete(inviteCode);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(instanceOf(ExistingUserInviteCompleter.class)));
    }

    @Test
    public void shouldResolve_selfSignupInviteDispatcher() {
        InviteEntity inviteEntity = anInvite(NEW_USER_AND_NEW_SERVICE_SELF_SIGNUP);
        when(inviteServiceFactory.dispatchServiceOtp()).thenReturn(new ServiceOtpDispatcher(null, null, null, null));
        InviteOtpDispatcher otpDispatcher = inviteRouter.routeOtpDispatch(inviteEntity);
        
        assertThat(otpDispatcher, is(instanceOf(ServiceOtpDispatcher.class)));
    }

    @Test
    public void shouldResolve_newUserExistingServiceInviteDispatcher() {
        InviteEntity inviteEntity = anInvite(NEW_USER_INVITED_TO_EXISTING_SERVICE);
        when(inviteServiceFactory.dispatchUserOtp()).thenReturn(new UserOtpDispatcher(null, null, null, null));
        InviteOtpDispatcher otpDispatcher = inviteRouter.routeOtpDispatch(inviteEntity);
        
        assertThat(otpDispatcher, is(instanceOf(UserOtpDispatcher.class)));
    }

    @Test
    public void shouldResolve_existingUserInviteDispatcher() {
        InviteEntity inviteEntity = anInvite(EXISTING_USER_INVITED_TO_EXISTING_SERVICE);

        assertThrows(IllegalArgumentException.class, () -> inviteRouter.routeOtpDispatch(inviteEntity));
    }

    private InviteEntity anInvite(InviteType inviteType) {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setCode("a-code");
        inviteEntity.setEmail("example@example.com");
        inviteEntity.setTelephoneNumber("+441134960000");
        inviteEntity.setOtpKey("u73t2b7");
        inviteEntity.setType(inviteType);
        return inviteEntity;
    }
}
