package uk.gov.pay.adminusers.persistence.entity;

import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class InviteExpiryTest {

    private InviteEntity inviteEntity;

    @Before
    public void setup() {
        inviteEntity = anInvite();
    }

    @Test
    public void isExpired_shouldNotBeExpire_whenRecentlyCreated() throws Exception {
        assertThat(inviteEntity.isExpired(), is(false));
    }

    @Test
    public void isExpired_shouldBeExpire_whenExpiryDateIsInThePast() throws Exception {

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
