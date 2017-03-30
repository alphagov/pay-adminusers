package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalUnit;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

public class InviteDbFixture {

    private DatabaseTestHelper databaseTestHelper;
    private String email = randomAlphanumeric(5) + "-invite@example.com";
    private ZonedDateTime date = ZonedDateTime.now(ZoneId.of("UTC"));
    private String code = randomAlphanumeric(100);

    private InviteDbFixture(DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }

    public static InviteDbFixture inviteDbFixture(DatabaseTestHelper databaseHelper) {
        return new InviteDbFixture(databaseHelper);
    }

    public InviteDbFixture expired() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(DAYS)
                .minus(1, DAYS)
                .minus(1, SECONDS);
        return this;
    }

    public String insertInvite() {
        //databaseTestHelper.add(invite(nextInt(), code, email, date), userId);
        //return inviteCode;
        return null;
    }
}
