package uk.gov.pay.adminusers.fixtures;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceName;
import uk.gov.pay.adminusers.model.ServiceRole;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class UserDbFixture {

    private final DatabaseTestHelper databaseTestHelper;
    private List<Pair<Service, Role>> serviceRolePairs = new ArrayList<>();
    private String externalId = randomUuid();
    private String username = randomUuid();
    private String otpKey = RandomStringUtils.randomAlphabetic(10);
    private String password = "password-" + username;
    private String email = username + "@example.com";
    private String telephoneNumber = "+447700900000";
    private String features = "FEATURE_1, FEATURE_2";
    private String provisionalOtpKey;
    private SecondFactorMethod secondFactorMethod = SecondFactorMethod.SMS;

    private UserDbFixture(DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }

    public static UserDbFixture userDbFixture(DatabaseTestHelper databaseTestHelper) {
        return new UserDbFixture(databaseTestHelper);
    }

    public User insertUser() {
        List<ServiceRole> serviceRoles = serviceRolePairs.stream().map(servicePair -> ServiceRole.from(servicePair.getLeft(), servicePair.getRight())).collect(Collectors.toList());
        User user = User.from(randomInt(), externalId, username, password, email, otpKey, telephoneNumber,
                serviceRoles, features, secondFactorMethod, provisionalOtpKey, null, null);

        databaseTestHelper.add(user);
        serviceRoles.forEach(serviceRole -> databaseTestHelper.addUserServiceRole(user.getId(), serviceRole.getService().getId(), serviceRole.getRole().getId()));

        return user;
    }

    public UserDbFixture withServiceRole(int serviceId, int roleId) {
        this.serviceRolePairs.add(Pair.of(Service.from(serviceId, randomUuid(), new ServiceName(Service.DEFAULT_NAME_VALUE)),
                Role.role(roleId, "roleName", "roleDescription")));
        return this;
    }

    public UserDbFixture withServiceRole(Service service, int roleId) {
        this.serviceRolePairs.add(Pair.of(service, Role.role(roleId, "roleName", "roleDescription")));
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

    public UserDbFixture withFeatures(String features) {
        this.features = features;
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
    
    public UserDbFixture withProvisionalOtpKey(String provisionalOtpKey) {
        this.provisionalOtpKey = provisionalOtpKey;
        return this;
    }

    public UserDbFixture withSecondFactorMethod(SecondFactorMethod secondFactorMethod) {
        this.secondFactorMethod = secondFactorMethod;
        return this;
    }
}
