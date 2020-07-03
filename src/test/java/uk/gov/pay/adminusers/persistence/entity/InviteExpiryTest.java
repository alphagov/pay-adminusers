package uk.gov.pay.adminusers.persistence.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class InviteExpiryTest {

    private InviteEntity inviteEntity;

    @BeforeEach
    public void setUp() {
        inviteEntity = anInvite();
    }

    @Test
    public void isExpired_shouldNotBeExpire_whenRecentlyCreated() {
        assertThat(inviteEntity.isExpired(), is(false));
    }

    @Test
    public void isExpired_shouldBeExpire_whenExpiryDateIsInThePast() {

        inviteEntity.setExpiryDate(ZonedDateTime.now(ZoneId.of("UTC")).minus(1, SECONDS));
        assertThat(inviteEntity.isExpired(), is(true));
    }

    private InviteEntity anInvite() {
        InviteEntity inviteEntity = new InviteEntity("user@example.com", "code", "otpKey", new RoleEntity());
        inviteEntity.setService(new ServiceEntity());
        inviteEntity.setSender(new UserEntity());
        return inviteEntity;

    }
}
