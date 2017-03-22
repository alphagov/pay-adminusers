package uk.gov.pay.adminusers.fixtures;

import org.apache.commons.lang3.RandomStringUtils;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class UserDbFixture {

    private final DatabaseTestHelper databaseTestHelper;
    private Integer serviceId;
    private Integer roleId;
    private String externalId = randomUuid();
    private String username = RandomStringUtils.randomAlphabetic(10);
    private String otpKey = RandomStringUtils.randomAlphabetic(10);
    private String password = "password-" + username;
    private String email= username + "@example.com";
    private String telephoneNumber = "374628482";

    private UserDbFixture(DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }

    public static UserDbFixture userDbFixture(DatabaseTestHelper databaseTestHelper) {
        return new UserDbFixture(databaseTestHelper);
    }

    public User insertUser() {
        if (serviceId == null) {
            serviceId = ServiceDbFixture.serviceDbFixture(databaseTestHelper).insertService();
            roleId = RoleDbFixture.roleDbFixture(databaseTestHelper).insertRole().getId();
        }
        User user = User.from(randomInt(), externalId, username, password, email, newArrayList(), asList(valueOf(serviceId)), otpKey, telephoneNumber);
        databaseTestHelper.add(user, roleId);
        return user;
    }

    public UserDbFixture withExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public UserDbFixture withServiceRole(int serviceId, int roleId) {
        this.serviceId = serviceId;
        this.roleId = roleId;
        return this;
    }

    public UserDbFixture withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserDbFixture withOtpKey(String otpKey) {
        this.otpKey = otpKey;
        return this;
    }
}
