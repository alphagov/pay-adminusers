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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toUnmodifiableList;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class UserDbFixture {

    private final DatabaseTestHelper databaseTestHelper;
    private List<Pair<Service, Role>> serviceRolePairs = new ArrayList<>();
    private String externalId = randomUuid();
    private String otpKey = RandomStringUtils.randomAlphabetic(10);
    private String password = "password-" + randomUuid();
    private String email = randomUuid() + "@example.com";
    private String telephoneNumber = "+447700900000";
    private String features = "FEATURE_1, FEATURE_2";
    private String provisionalOtpKey;
    private SecondFactorMethod secondFactorMethod = SecondFactorMethod.SMS;
    private ZonedDateTime createdAt;
    private ZonedDateTime lastLoggedInAt;

    private UserDbFixture(DatabaseTestHelper databaseTestHelper) {
        this.databaseTestHelper = databaseTestHelper;
    }

    public static UserDbFixture userDbFixture(DatabaseTestHelper databaseTestHelper) {
        return new UserDbFixture(databaseTestHelper);
    }

    public User insertUser() {
        List<ServiceRole> serviceRoles = serviceRolePairs.stream()
                .map(servicePair -> ServiceRole.from(servicePair.getLeft(), servicePair.getRight()))
                .collect(toUnmodifiableList());

        User user = User.from(randomInt(), externalId, password, email, otpKey, telephoneNumber,
                serviceRoles, features, secondFactorMethod, provisionalOtpKey, null, lastLoggedInAt, createdAt);

        databaseTestHelper.add(user);
        serviceRoles.forEach(serviceRole -> 
                databaseTestHelper.addUserServiceRole(user.getId(), serviceRole.getService().getId(), serviceRole.getRole().getId()));

        return user;
    }

    public UserDbFixture withServiceRole(int serviceId, Role role) {
        this.serviceRolePairs.add(Pair.of(Service.from(serviceId, randomUuid(), new ServiceName(Service.DEFAULT_NAME_VALUE)),
                role));
        return this;
    }

    public UserDbFixture withServiceRole(Service service, Role role) {
        this.serviceRolePairs.add(Pair.of(service, role));
        return this;
    }

    public UserDbFixture withExternalId(String externalId) {
        this.externalId = externalId;
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

    public UserDbFixture withCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public UserDbFixture withLastLoggedInAt(ZonedDateTime lastLoggedInAt) {
        this.lastLoggedInAt = lastLoggedInAt;
        return this;
    }
}
