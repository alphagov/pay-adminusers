package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;

public class ForgottenPasswordDbFixture {

    private DatabaseTestHelper databaseTestHelper;
    private int userId;
    private ZonedDateTime date = now(ZoneId.of("UTC"));
    private String forgottenPasswordCode = randomAlphanumeric(100);
    private ZonedDateTime createdAt = now(ZoneId.of("UTC"));

    private final Integer id = nextInt();

    public static ForgottenPasswordDbFixture aForgottenPasswordDbFixture() {
        return new ForgottenPasswordDbFixture();
    }

    public ForgottenPasswordDbFixture insert() {
        databaseTestHelper.insertForgottenPassword(id, date, forgottenPasswordCode, userId, createdAt);
        return this;
    }

    public ForgottenPasswordDbFixture withExpiryDate(ZonedDateTime expiryDate) {
        this.date = expiryDate;
        return this;
    }

    public ForgottenPasswordDbFixture withCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ForgottenPasswordDbFixture withUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public ForgottenPasswordDbFixture withCode(String forgottenPasswordCode) {
        this.forgottenPasswordCode = forgottenPasswordCode;
        return this;
    }

    public ForgottenPasswordDbFixture withDatabaseTestHelper(DatabaseTestHelper databaseHelper) {
        this.databaseTestHelper = databaseHelper;
        return this;
    }

    public String getForgottenPasswordCode() {
        return forgottenPasswordCode;
    }

    public ZonedDateTime getDate() {
        return date;
    }

}
