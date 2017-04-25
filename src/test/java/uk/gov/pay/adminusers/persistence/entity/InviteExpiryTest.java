package uk.gov.pay.adminusers.persistence.entity;

import org.junit.Test;

import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class InviteExpiryTest {

    @Test
    public void isExpired_shouldNotBeExpire_whenRecentlyCreated() throws Exception {

        InviteEntity inviteEntity = new InviteEntity("user@example.com", "code", new UserEntity(), new ServiceEntity(), new RoleEntity());

        assertThat(inviteEntity.isExpired(), is(false));
    }

    @Test
    /**
     *  Being:
     *
     *  'X' the moment the invite is created and
     *  '|' = 00:00 of the following day
     *  '^' the moment it expires
     *  'N' = Now
     *
     *   <-------Day 0---------><-------Day 1---------><-------Day 2--------->
     *   |----------------------|----------------------|----------------------|
     *                          X                               N             ^
     *
     *  Invite created Day 1 -> 00:00:00:000 will expired at Day 3 -> 00:00:00:000
     */
    public void isExpired_shouldNotExpireWhenCreatedOnMidnightTheDayBefore() {

        InviteEntity inviteEntity = new InviteEntity("user@example.com", "code", new UserEntity(), new ServiceEntity(), new RoleEntity());
        inviteEntity.setDate(ZonedDateTime.now().truncatedTo(DAYS).minus(1, DAYS));

        assertThat(inviteEntity.isExpired(), is(false));
    }

    @Test
    /**
     *  Being:
     *
     *  'X' the moment the invite is created and
     *  '|' = 00:00:00 of the following day
     *  '^' the moment it expires
     *  'N' = Now
     *
     *   <-------Day 0---------><-------Day 1---------><-------Day 2--------->
     *   |----------------------|----------------------|----------------------|
     *                         X                       ^    N
     *
     *   Invite created Day 0 -> 23:59:59:999 will expired at Day 2 -> 00:00:00:000
     */
    public void isExpired_shouldExpireWhenCreatedTwoDaysBeforeNowOneMillisecondBeforeMidnight() {

        InviteEntity inviteEntity = new InviteEntity("user@example.com", "code", new UserEntity(), new ServiceEntity(), new RoleEntity());
        inviteEntity.setDate(ZonedDateTime.now().truncatedTo(DAYS).minus(1, DAYS).minus(1, MILLIS));

        assertThat(inviteEntity.isExpired(), is(true));
    }
}
