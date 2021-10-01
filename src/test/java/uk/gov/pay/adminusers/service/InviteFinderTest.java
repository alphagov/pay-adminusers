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
import java.time.ZoneOffset;
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

    private static final String EMAIL = "test@test.gov.uk";
    private static final String CODE = "invite-code";
    private static final String OTP_KEY = "otp-key";

    @Mock
    private UserDao mockUserDao;
    @Mock
    private InviteDao mockInviteDao;
    @Mock
    private RoleEntity mockRoleEntity;

    private InviteFinder inviteFinder;

    @BeforeEach
    public void before() {
        inviteFinder = new InviteFinder(mockInviteDao, mockUserDao);
    }

    @Test
    public void shouldFindInvite_withNonExistingUser() {
        InviteEntity inviteEntity = new InviteEntity(EMAIL, CODE, OTP_KEY, mockRoleEntity);
        when(mockInviteDao.findByCode(CODE)).thenReturn(Optional.of(inviteEntity));
        when(mockUserDao.findByEmail(EMAIL)).thenReturn(Optional.empty());

        Optional<Invite> inviteOptional = inviteFinder.find(CODE);
        assertThat(inviteOptional.isPresent(), is(true));
        assertThat(inviteOptional.get().isUserExist(),is(false));
    }

    @Test
    public void shouldFindInvite_withExistingUser() {
        InviteEntity inviteEntity = new InviteEntity(EMAIL, CODE, OTP_KEY, mockRoleEntity);
        when(mockInviteDao.findByCode(CODE)).thenReturn(Optional.of(inviteEntity));
        when(mockUserDao.findByEmail(EMAIL)).thenReturn(Optional.of(mock(UserEntity.class)));

        Optional<Invite> inviteOptional = inviteFinder.find(CODE);
        assertThat(inviteOptional.isPresent(), is(true));
        assertThat(inviteOptional.get().isUserExist(),is(true));
    }

    @Test
    public void shouldHaveFlagToSayPasswordNotSet() {
        InviteEntity inviteEntity = new InviteEntity(EMAIL, CODE, OTP_KEY, mockRoleEntity);
        when(mockInviteDao.findByCode(CODE)).thenReturn(Optional.of(inviteEntity));

        Optional<Invite> inviteOptional = inviteFinder.find(CODE);
        assertThat(inviteOptional.isPresent(), is(true));
        assertThat(inviteOptional.get().isPasswordSet(), is(false));
    }

    @Test
    public void shouldHaveFlagToSayPasswordIsSet() {
        InviteEntity inviteEntity = new InviteEntity(EMAIL, CODE, OTP_KEY, mockRoleEntity);
        inviteEntity.setPassword("password123");
        when(mockInviteDao.findByCode(CODE)).thenReturn(Optional.of(inviteEntity));

        Optional<Invite> inviteOptional = inviteFinder.find(CODE);
        assertThat(inviteOptional.isPresent(), is(true));
        assertThat(inviteOptional.get().isPasswordSet(), is(true));
    }

    @Test
    public void shouldErrorLocked_ifInviteIsExpired() {
        InviteEntity inviteEntity = new InviteEntity(EMAIL, CODE, OTP_KEY, mockRoleEntity);
        inviteEntity.setExpiryDate(ZonedDateTime.now(ZoneOffset.UTC).minusDays(1));
        when(mockInviteDao.findByCode(CODE)).thenReturn(Optional.of(inviteEntity));

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> inviteFinder.find(CODE));
        assertThat(exception.getMessage(), is("HTTP 410 Gone"));
    }

    @Test
    public void shouldErrorLocked_ifInviteIsDisabled() {
        InviteEntity inviteEntity = new InviteEntity(EMAIL, CODE, OTP_KEY, mockRoleEntity);
        inviteEntity.setDisabled(true);
        Optional<InviteEntity> inviteEntityOptional = Optional.of(inviteEntity);
        when(mockInviteDao.findByCode(CODE)).thenReturn(inviteEntityOptional);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> inviteFinder.find(CODE));
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
        InviteEntity firstInviteEntity = new InviteEntity(firstEmail, randomUuid(), OTP_KEY, mock(RoleEntity.class));
        InviteEntity secondInviteEntity = new InviteEntity(secondEmail, randomUuid(), OTP_KEY, mock(RoleEntity.class));
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
