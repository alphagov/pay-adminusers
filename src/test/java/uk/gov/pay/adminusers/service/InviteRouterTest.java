package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

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
        inviteRouter = new InviteRouter(inviteServiceFactory);
    }

    @Test
    public void shouldResolve_selfSignupInviteCompleter_withValidation() {
        InviteEntity inviteEntity = anInvite(SERVICE);
        when(inviteServiceFactory.completeSelfSignupInvite()).thenReturn(new SelfSignupInviteCompleter(null, null, null, null));
        InviteCompleter inviteCompleter = inviteRouter.routeComplete(inviteEntity);
        
        assertThat(inviteCompleter, is(instanceOf(SelfSignupInviteCompleter.class)));
    }

    @Test
    public void shouldResolve_existingUserInviteCompleter_withoutValidation() {
        InviteEntity inviteEntity = anInvite(USER);
        when(inviteServiceFactory.completeExistingUserInvite()).thenReturn(new ExistingUserInviteCompleter(null, null));
        InviteCompleter inviteCompleter = inviteRouter.routeComplete(inviteEntity);
        
        assertThat(inviteCompleter, is(instanceOf(ExistingUserInviteCompleter.class)));
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
        InviteEntity inviteEntity = anInvite(NEW_USER_AND_NEW_SERVICE_SELF_SIGNUP);
        when(inviteServiceFactory.completeSelfSignupInvite()).thenReturn(new SelfSignupInviteCompleter(null, null, null, null));
        InviteCompleter inviteCompleter = inviteRouter.routeComplete(inviteEntity);
        
        assertThat(inviteCompleter, is(instanceOf(SelfSignupInviteCompleter.class)));
    }

    @Test
    public void shouldResolve_newUserExistingServiceInviteCompleter() {
        InviteEntity inviteEntity = anInvite(NEW_USER_INVITED_TO_EXISTING_SERVICE);
        when(inviteServiceFactory.completeNewUserExistingServiceInvite()).thenReturn(new NewUserExistingServiceInviteCompleter(null, null, null));
        InviteCompleter inviteCompleter = inviteRouter.routeComplete(inviteEntity);

        assertThat(inviteCompleter, is(instanceOf(NewUserExistingServiceInviteCompleter.class)));
    }

    @Test
    public void shouldResolve_existingUserInviteCompleter() {
        InviteEntity inviteEntity = anInvite(EXISTING_USER_INVITED_TO_EXISTING_SERVICE);
        when(inviteServiceFactory.completeExistingUserInvite()).thenReturn(new ExistingUserInviteCompleter(null, null));
        InviteCompleter inviteCompleter = inviteRouter.routeComplete(inviteEntity);
        
        assertThat(inviteCompleter, is(instanceOf(ExistingUserInviteCompleter.class)));
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
