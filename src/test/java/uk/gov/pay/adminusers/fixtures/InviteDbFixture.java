package uk.gov.pay.adminusers.fixtures;

import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class InviteDbFixture {

    private DatabaseTestHelper databaseTestHelper;
    private String email = randomAlphanumeric(5) + "-invite@example.com";
    private ZonedDateTime date = ZonedDateTime.now(ZoneId.of("UTC"));
    private ZonedDateTime expiryDate = this.date.plus(1, DAYS);
    private String code = randomAlphanumeric(100);
    private String otpKey = "KPWXGUTNWOE7PMVK";
    private String telephoneNumber;
    private String password;
    private Boolean disabled = Boolean.FALSE;
    private Integer loginCounter = 0;
    private String externalServiceId = randomUuid();
    private Integer serviceId = randomInt();

    private InviteDbFixture(DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }

    public static InviteDbFixture inviteDbFixture(DatabaseTestHelper databaseHelper) {
        return new InviteDbFixture(databaseHelper);
    }

    public String insertInviteToAddUserToService() {
        ServiceDbFixture.serviceDbFixture(databaseTestHelper).withId(serviceId).withExternalId(externalServiceId).insertService().getId();
        int roleId = RoleDbFixture.roleDbFixture(databaseTestHelper).insertRole().getId();
        String userUsername = randomUuid();
        String userEmail = userUsername + "@example.com";
        int invitingUserId =
                UserDbFixture.userDbFixture(databaseTestHelper)
                        .withUsername(userUsername)
                        .withEmail(userEmail)
                        .insertUser()
                        .getId();
        databaseTestHelper.addInvite(
                nextInt(), invitingUserId, serviceId, roleId,
                email, code, otpKey, date, expiryDate, telephoneNumber, password,
                disabled, loginCounter
        );
        return code;
    }

    public String insertSelfSignupInvite() {
        int roleId = RoleDbFixture.roleDbFixture(databaseTestHelper).insertRole().getId();
        String userUsername = randomUuid();
        String userEmail = userUsername + "@example.com";
        int invitingUserId =
                UserDbFixture.userDbFixture(databaseTestHelper)
                        .withUsername(userUsername)
                        .withEmail(userEmail)
                        .insertUser()
                        .getId();
        databaseTestHelper.addServiceInvite(
                nextInt(), invitingUserId, roleId,
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

    public InviteDbFixture withServiceExternalId(String serviceExternalId) {
        this.externalServiceId = serviceExternalId;
        return this;
    }

    public InviteDbFixture withServiceId(Integer serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public InviteDbFixture withCode(String code) {
        this.code = code;
        return this;
    }
}
