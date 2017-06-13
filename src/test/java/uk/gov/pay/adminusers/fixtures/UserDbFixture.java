package uk.gov.pay.adminusers.fixtures;

import org.apache.commons.lang3.RandomStringUtils;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class UserDbFixture {

    private final DatabaseTestHelper databaseTestHelper;
    private Service service;
    private Integer roleId;
    private String externalId = randomUuid();
    private String username = RandomStringUtils.randomAlphabetic(10);
    private String otpKey = RandomStringUtils.randomAlphabetic(10);
    private String password = "password-" + username;
    private String email = username + "@example.com";
    private String telephoneNumber = "374628482";
    private List<String> gatewayAccountIds = newArrayList();

    private UserDbFixture(DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }

    public static UserDbFixture userDbFixture(DatabaseTestHelper databaseTestHelper) {
        return new UserDbFixture(databaseTestHelper);
    }

    public User insertUser() {
        if (service == null) {
            service = ServiceDbFixture.serviceDbFixture(databaseTestHelper).insertService();
            roleId = RoleDbFixture.roleDbFixture(databaseTestHelper).insertRole().getId();
        }
        User user = User.from(randomInt(), externalId, username, password, email, gatewayAccountIds, asList(service), otpKey, telephoneNumber);
        databaseTestHelper.add(user, roleId);
        return user;
    }

    @Deprecated // May be removed when all moved to using serviceExternalId instead of serviceId
    public UserDbFixture withServiceRole(int serviceId, int roleId) {
        this.service = Service.from(serviceId, randomUuid(), Service.DEFAULT_NAME_VALUE);
        this.roleId = roleId;
        return this;
    }

    public UserDbFixture withServiceRole(Service service, int roleId) {
        this.service = service;
        this.roleId = roleId;
        return this;
    }

    public UserDbFixture withExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public UserDbFixture withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserDbFixture withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserDbFixture withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserDbFixture withGatewayAccountIds(List<String> gatewayAccountIds) {
        this.gatewayAccountIds = gatewayAccountIds;
        return this;
    }

    public UserDbFixture withTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
        return this;
    }

    public UserDbFixture withOtpKey(String otpKey) {
        this.otpKey = otpKey;
        return this;
    }
}
