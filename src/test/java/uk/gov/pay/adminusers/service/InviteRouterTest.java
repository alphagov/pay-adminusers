package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.InviteType;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceName;
import uk.gov.pay.adminusers.model.ServiceRole;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.InviteType.SERVICE;
import static uk.gov.pay.adminusers.model.InviteType.USER;
import static uk.gov.pay.adminusers.model.Role.role;
import static uk.gov.pay.adminusers.persistence.entity.Role.ADMIN;

@ExtendWith(MockitoExtension.class)
public class InviteRouterTest {

    @Mock
    private InviteServiceFactory inviteServiceFactory;

    @Mock
    private UserServices userService;

    private InviteRouter inviteRouter;
    
    private static final String email = "example@example.com";

    @BeforeEach
    public void before() {
        inviteRouter = new InviteRouter(inviteServiceFactory, userService);
    }

    @Test
    public void shouldResolve_selfSignupInviteCompleter_forServiceInviteType() {
        InviteEntity inviteEntity = anInvite(SERVICE);
        when(inviteServiceFactory.completeSelfSignupInvite()).thenReturn(new SelfSignupInviteCompleter(null, null, null, null));
        InviteCompleter inviteCompleter = inviteRouter.routeComplete(inviteEntity);
        
        assertThat(inviteCompleter, is(instanceOf(SelfSignupInviteCompleter.class)));
    }

    @Test
    public void shouldResolve_existingUserInviteCompleter_forUserInviteType_whenUserAlreadyExists() {
        InviteEntity inviteEntity = anInvite(USER);
        when(userService.findUserByEmail(email)).thenReturn(Optional.of(aUser(email)));
        when(inviteServiceFactory.completeExistingUserInvite()).thenReturn(new ExistingUserInviteCompleter(null, null));
        InviteCompleter inviteCompleter = inviteRouter.routeComplete(inviteEntity);

        assertThat(inviteCompleter, is(instanceOf(ExistingUserInviteCompleter.class)));
    }

    @Test
    public void shouldResolve_newUserExistingServiceInviteCompleter_forUserInviteType_whenUserDoesNotExist() {
        InviteEntity inviteEntity = anInvite(USER);
        when(userService.findUserByEmail(email)).thenReturn(Optional.empty());
        when(inviteServiceFactory.completeNewUserExistingServiceInvite()).thenReturn(new NewUserExistingServiceInviteCompleter(null, null, null));
        InviteCompleter inviteCompleter = inviteRouter.routeComplete(inviteEntity);

        assertThat(inviteCompleter, is(instanceOf(NewUserExistingServiceInviteCompleter.class)));
    }

    @Test
    public void shouldResolve_selfSignupInviteDispatcher_forServiceInviteType() {
        InviteEntity inviteEntity = anInvite(SERVICE);
        when(inviteServiceFactory.dispatchServiceOtp()).thenReturn(new ServiceOtpDispatcher(null, null, null, null));
        InviteOtpDispatcher otpDispatcher = inviteRouter.routeOtpDispatch(inviteEntity);

        assertThat(otpDispatcher, is(instanceOf(ServiceOtpDispatcher.class)));
    }

    @Test
    public void shouldResolve_existingUserInviteDispatcher_forUserInviteType() {
        InviteEntity inviteEntity = anInvite(USER);
        when(inviteServiceFactory.dispatchUserOtp()).thenReturn(new UserOtpDispatcher(null, null, null, null));
        InviteOtpDispatcher otpDispatcher = inviteRouter.routeOtpDispatch(inviteEntity);
        
        assertThat(otpDispatcher, is(instanceOf(UserOtpDispatcher.class)));
    }

    private InviteEntity anInvite(InviteType inviteType) {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setCode("a-code");
        inviteEntity.setEmail(email);
        inviteEntity.setTelephoneNumber("+441134960000");
        inviteEntity.setOtpKey("u73t2b7");
        inviteEntity.setType(inviteType);
        return inviteEntity;
    }

    private User aUser(String email) {
        Service service = Service.from(1, "3453rmeuty87t", new ServiceName(Service.DEFAULT_NAME_VALUE));
        ServiceRole serviceRole = ServiceRole.from(service, role(ADMIN.getId(), "Admin", "Administrator"));
        return User.from(randomInt(), randomUuid(), "a-username", "random-password", email,
                "784rh", "8948924", Collections.singletonList(serviceRole), null,
                SecondFactorMethod.SMS, null, null, null);
    }
}
