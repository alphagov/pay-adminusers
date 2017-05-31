package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;

public class InviteDbFixture {

    private DatabaseTestHelper databaseTestHelper;
    private String email = randomAlphanumeric(5) + "-invite@example.com";
    private ZonedDateTime date = ZonedDateTime.now(ZoneId.of("UTC"));
    private ZonedDateTime expiryDate = this.date.plus(1, DAYS);
    private String code = randomAlphanumeric(100);
    private String otpKey = randomAlphanumeric(100);
    private String telephoneNumber;
    private String password;
    private Boolean disabled = Boolean.FALSE;
    private Integer loginCounter = 0;

    private InviteDbFixture(DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }

    public static InviteDbFixture inviteDbFixture(DatabaseTestHelper databaseHelper) {
        return new InviteDbFixture(databaseHelper);
    }

    public String insertInvite() {
        int serviceId = ServiceDbFixture.serviceDbFixture(databaseTestHelper).insertService().getId();
        int roleId = RoleDbFixture.roleDbFixture(databaseTestHelper).insertRole().getId();
        int userId = UserDbFixture.userDbFixture(databaseTestHelper).insertUser().getId();
        databaseTestHelper.addInvite(
                nextInt(), userId, serviceId, roleId,
                email, code, otpKey, date, expiryDate, telephoneNumber, password,
                disabled, loginCounter
        );
        return code;
    }

    public InviteDbFixture expired() {
        this.expiryDate = this.date.minus(1, SECONDS);
        return this;
    }

    public InviteDbFixture disabled() {
        this.disabled = Boolean.TRUE;
        return this;
    }

    public InviteDbFixture withLoginCounter(Integer loginCounter) {
        this.loginCounter = loginCounter;
        return this;
    }

    public InviteDbFixture withTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
        return this;
    }

    public InviteDbFixture withEmail(String email) {
        this.email = email;
        return this;
    }

    public InviteDbFixture withOtpKey(String otpKey) {
        this.otpKey = otpKey;
        return this;
    }

    public InviteDbFixture withPassword(String password) {
        this.password = password;
        return this;
    }
}
