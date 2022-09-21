package uk.gov.pay.adminusers.service;

import org.apache.commons.lang3.tuple.Pair;
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
import static org.mockito.Mockito.when;
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
        InviteEntity inviteEntity = anInvite(inviteCode, SERVICE);
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(inviteServiceFactory.completeSelfSignupInvite()).thenReturn(new SelfSignupInviteCompleter(null, null, null, null));
        Optional<InviteCompleter> result = inviteRouter.routeComplete(inviteCode);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(instanceOf(SelfSignupInviteCompleter.class)));
    }

    @Test
    public void shouldResolve_existingUserInviteCompleter_withoutValidation() {
        String inviteCode = "a-code";
        InviteEntity inviteEntity = anInvite(inviteCode, USER);
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(inviteServiceFactory.completeExistingUserInvite()).thenReturn(new ExistingUserInviteCompleter(null, null));
        Optional<InviteCompleter> result = inviteRouter.routeComplete(inviteCode);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(instanceOf(ExistingUserInviteCompleter.class)));
    }

    @Test
    public void shouldResolve_existingUserInviteDispatcher_withValidation() {
        String inviteCode = "a-code";
        InviteEntity inviteEntity = anInvite(inviteCode, USER);
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(inviteServiceFactory.dispatchUserOtp()).thenReturn(new UserOtpDispatcher(null, null, null, null));
        Optional<Pair<InviteOtpDispatcher, Boolean>> result = inviteRouter.routeOtpDispatch(inviteCode);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getLeft(), is(instanceOf(UserOtpDispatcher.class)));
        assertThat(result.get().getRight(), is(true));
    }

    @Test
    public void shouldResolve_selfSignupInviteDispatcher_withoutValidation() {
        String inviteCode = "a-code";
        InviteEntity inviteEntity = anInvite(inviteCode, SERVICE);
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(inviteServiceFactory.dispatchServiceOtp()).thenReturn(new ServiceOtpDispatcher(null, null, null, null));
        Optional<Pair<InviteOtpDispatcher, Boolean>> result = inviteRouter.routeOtpDispatch(inviteCode);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getLeft(), is(instanceOf(ServiceOtpDispatcher.class)));
        assertThat(result.get().getRight(), is(false));
    }

    private InviteEntity anInvite(String code, InviteType inviteType) {
        InviteEntity inviteEntity = new InviteEntity();
        inviteEntity.setCode(code);
        inviteEntity.setEmail("example@example.com");
        inviteEntity.setTelephoneNumber("+441134960000");
        inviteEntity.setOtpKey("u73t2b7");
        inviteEntity.setType(inviteType);
        return inviteEntity;
    }
}
