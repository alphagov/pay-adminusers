package uk.gov.pay.adminusers.service;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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

@RunWith(MockitoJUnitRunner.class)
public class InviteRouterTest {

    @Mock
    private InviteDao inviteDao;

    @Mock
    private InviteServiceFactory inviteServiceFactory;

    private InviteRouter inviteRouter;

    @Before
    public void before() {
        inviteRouter = new InviteRouter(inviteServiceFactory, inviteDao);
    }

    @Test
    public void shouldResolve_serviceInviteCompleter_withValidation() {
        String inviteCode = "a-code";
        InviteEntity inviteEntity = anInvite(inviteCode, SERVICE);
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(inviteServiceFactory.completeServiceInvite()).thenReturn(new ServiceInviteCompleter(null, null, null, null));
        Optional<Pair<InviteCompleter, Boolean>> result = inviteRouter.routeComplete(inviteCode);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getLeft(), is(instanceOf(ServiceInviteCompleter.class)));
        assertThat(result.get().getRight(), is(true));
    }

    @Test
    public void shouldResolve_userInviteCompleter_withoutValidation() {
        String inviteCode = "a-code";
        InviteEntity inviteEntity = anInvite(inviteCode, USER);
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(inviteServiceFactory.completeUserInvite()).thenReturn(new UserInviteCompleter(null, null));
        Optional<Pair<InviteCompleter, Boolean>> result = inviteRouter.routeComplete(inviteCode);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getLeft(), is(instanceOf(UserInviteCompleter.class)));
        assertThat(result.get().getRight(), is(false));
    }

    @Test
    public void shouldResolve_userInviteDispatcher_withValidation() {
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
    public void shouldResolve_serviceInviteDispatcher_withoutValidation() {
        String inviteCode = "a-code";
        InviteEntity inviteEntity = anInvite(inviteCode, SERVICE);
        when(inviteDao.findByCode(inviteCode)).thenReturn(Optional.of(inviteEntity));
        when(inviteServiceFactory.dispatchServiceOtp()).thenReturn(new ServiceOtpDispatcher(null, null, null));
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
