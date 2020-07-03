package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.ws.rs.WebApplicationException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

@ExtendWith(MockitoExtension.class)
public class InviteFinderTest {

    @Mock
    private UserDao mockUserDao;
    @Mock
    private InviteDao mockInviteDao;

    private InviteFinder inviteFinder;

    @BeforeEach
    public void before() {
        inviteFinder = new InviteFinder(mockInviteDao, mockUserDao);
    }

    @Test
    public void shouldFindInvite_withNonExistingUser() {
        String code = randomUuid();
        String email = "user@mail.com";
        InviteEntity inviteEntity = new InviteEntity(email, code, "otp-key", mock(RoleEntity.class));
        Optional<InviteEntity> inviteEntityOptional = Optional.of(inviteEntity);
        when(mockInviteDao.findByCode(code)).thenReturn(inviteEntityOptional);
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.empty());

        Optional<Invite> inviteOptional = inviteFinder.find(code);
        assertThat(inviteOptional.isPresent(), is(true));
        assertThat(inviteOptional.get().isUserExist(),is(false));
    }

    @Test
    public void shouldFindInvite_withExistingUser() {
        String code = randomUuid();
        String email = "user@mail.com";
        InviteEntity inviteEntity = new InviteEntity(email, code, "otp-key", mock(RoleEntity.class));
        Optional<InviteEntity> inviteEntityOptional = Optional.of(inviteEntity);
        when(mockInviteDao.findByCode(code)).thenReturn(inviteEntityOptional);
        when(mockUserDao.findByEmail(email)).thenReturn(Optional.of(mock(UserEntity.class)));

        Optional<Invite> inviteOptional = inviteFinder.find(code);
        assertThat(inviteOptional.isPresent(), is(true));
        assertThat(inviteOptional.get().isUserExist(),is(true));
    }

    @Test
    public void shouldErrorLocked_ifInviteIsExpired() {
        String code = randomUuid();
        String email = "user@mail.com";
        InviteEntity inviteEntity = new InviteEntity(email, code, "otp-key", mock(RoleEntity.class));
        inviteEntity.setExpiryDate(ZonedDateTime.now().minusDays(1));
        Optional<InviteEntity> inviteEntityOptional = Optional.of(inviteEntity);
        when(mockInviteDao.findByCode(code)).thenReturn(inviteEntityOptional);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> inviteFinder.find(code));
        assertThat(exception.getMessage(), is("HTTP 410 Gone"));
    }

    @Test
    public void shouldErrorLocked_ifInviteIsDisabled() {
        String code = randomUuid();
        String email = "user@mail.com";
        InviteEntity inviteEntity = new InviteEntity(email, code, "otp-key", mock(RoleEntity.class));
        inviteEntity.setDisabled(true);
        Optional<InviteEntity> inviteEntityOptional = Optional.of(inviteEntity);
        when(mockInviteDao.findByCode(code)).thenReturn(inviteEntityOptional);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> inviteFinder.find(code));
        assertThat(exception.getMessage(), is("HTTP 410 Gone"));
    }

    @Test
    public void shouldReturnEmptyOptional_forNonExistingInviteCode() {
        String code = "non-existent-code";
        when(mockInviteDao.findByCode(code)).thenReturn(Optional.empty());

        Optional<Invite> inviteOptional = inviteFinder.find(code);

        assertThat(inviteOptional.isPresent(), is(false));
    }

    @Test
    public void shouldFindAllActiveInvites() {
        String externalServiceId = "sdfuhsdyftgdfa";
        String firstEmail = "user1@mail.test";
        String secondEmail = "user2@mail.test";
        InviteEntity firstInviteEntity = new InviteEntity(firstEmail, randomUuid(), "otp-key", mock(RoleEntity.class));
        InviteEntity secondInviteEntity = new InviteEntity(secondEmail, randomUuid(), "otp-key", mock(RoleEntity.class));
        InviteEntity disabledInviteEntity = new InviteEntity("email@email.test", randomUuid(), "otp-key", mock(RoleEntity.class));
        disabledInviteEntity.setDisabled(true);
        InviteEntity expiredInviteEntity = new InviteEntity("email@email.test", randomUuid(), "otp-key", mock(RoleEntity.class));
        expiredInviteEntity.setExpiryDate(ZonedDateTime.now().minusMinutes(1));
        when(mockUserDao.findByEmail(firstEmail)).thenReturn(Optional.empty());
        when(mockUserDao.findByEmail(secondEmail)).thenReturn(Optional.empty());
        when(mockInviteDao.findAllByServiceId(externalServiceId)).thenReturn(
                List.of(firstInviteEntity, secondInviteEntity, disabledInviteEntity, expiredInviteEntity)
        );
        List<Invite> invites = inviteFinder.findAllActiveInvites(externalServiceId);
        assertThat(invites.size(), is(2));
        Invite firstInvite = invites.get(0);
        assertThat(firstInvite.getEmail(), is(firstEmail));
        Invite secondInvite = invites.get(1);
        assertThat(secondInvite.getEmail(), is(secondEmail));
    }
}
