package uk.gov.pay.adminusers.fixtures;

import org.apache.commons.lang3.RandomStringUtils;
import uk.gov.pay.adminusers.app.util.RandomIdGenerator;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static uk.gov.pay.adminusers.model.ForgottenPassword.forgottenPassword;

public class ForgottenPasswordDbFixture {

    private final DatabaseTestHelper databaseTestHelper;
    private final int userId;
    private ZonedDateTime date = ZonedDateTime.now(ZoneId.of("UTC"));
    private final String forgottenPasswordCode = RandomStringUtils.randomAlphanumeric(100);

    private ForgottenPasswordDbFixture(DatabaseTestHelper databaseTestHelper, int userId) {
        this.databaseTestHelper = databaseTestHelper;
        this.userId = userId;
    }

    public static ForgottenPasswordDbFixture forgottenPasswordDbFixture(DatabaseTestHelper databaseHelper, int userId) {
        return new ForgottenPasswordDbFixture(databaseHelper, userId);
    }

    public String insertForgottenPassword() {
        databaseTestHelper.add(forgottenPassword(nextInt(), forgottenPasswordCode, date, RandomIdGenerator.randomUuid()), userId);
        return forgottenPasswordCode;
    }

    public ForgottenPasswordDbFixture expired() {
        date = ZonedDateTime.now(ZoneId.of("UTC")).minus(91, MINUTES);
        return this;
    }
}
