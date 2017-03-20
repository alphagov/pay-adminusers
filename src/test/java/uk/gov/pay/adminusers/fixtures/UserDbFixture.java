package uk.gov.pay.adminusers.fixtures;

import org.apache.commons.lang3.RandomStringUtils;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

public class UserDbFixture {


    private final DatabaseTestHelper databaseTestHelper;
    private Integer serviceId;
    private Integer roleId;

    public UserDbFixture(DatabaseTestHelper databaseTestHelper) {

        this.databaseTestHelper = databaseTestHelper;
    }

    public static UserDbFixture aUser(DatabaseTestHelper databaseTestHelper) {
        return new UserDbFixture(databaseTestHelper);
    }

    public User build() {
        String username = RandomStringUtils.randomAlphabetic(10);
        String otpKey = RandomStringUtils.randomAlphabetic(10);
        if(serviceId == null) {
            serviceId = ServiceDbFixture.aService(databaseTestHelper).build();
            roleId = RoleDbFixture.aRole(databaseTestHelper).build().getId();
        }
        User user = User.from(randomInt(), username, "password-" + username, username + "@example.com", newArrayList(), asList(valueOf(serviceId)), otpKey, "374628482");
        databaseTestHelper.add(user, roleId);
        return user;
    }

    public UserDbFixture withServiceRole(int serviceId, int roleId) {
        this.serviceId = serviceId;
        this.roleId = roleId;
        return this;
    }
}
