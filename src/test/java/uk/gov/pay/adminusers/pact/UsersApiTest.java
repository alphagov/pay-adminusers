package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactSource;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import uk.gov.pay.adminusers.app.util.RandomIdGenerator;
import uk.gov.pay.adminusers.fixtures.RoleDbFixture;
import uk.gov.pay.adminusers.fixtures.ServiceDbFixture;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;
import uk.gov.pay.adminusers.infra.DropwizardAppWithPostgresRule;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.service.PasswordHasher;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

@RunWith(PactRunner.class)
@Provider("adminusers")
@PactSource(ConfigurablePactLoader.class)
public class UsersApiTest {

    @ClassRule
    public static DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    private static final PasswordHasher passwordHasher = new PasswordHasher();
    private static DatabaseTestHelper dbHelper;

    @BeforeClass
    public static void setUpService() throws Exception {
        target = new HttpTarget(app.getLocalPort());
        dbHelper = app.getDatabaseTestHelper();
        // make sure we create services(including gateway account ids) before users
        serviceDbFixture(dbHelper).withGatewayAccountIds("268").insertService();
        int serviceId = 12345;
        createUserWithinAService("7d19aff33f8948deb97ed16b2912dcd3", "existing-user", serviceId, "password");
    }

    @TestTarget
    public static Target target;

    @State("a valid forgotten password entry and a related user exists")
    public void aUserExistsWithAForgottenPasswordRequest() throws Exception {
        String code = "avalidforgottenpasswordtoken";
        String userExternalId = RandomIdGenerator.randomUuid();
        createUserWithinAService(userExternalId, randomAlphabetic(20), "password");
        List<Map<String, Object>> userByExternalId = dbHelper.findUserByExternalId(userExternalId);
        dbHelper.add(ForgottenPassword.forgottenPassword(code, userExternalId), (Integer) userByExternalId.get(0).get("id"));
    }

    @State("a user exists with max login attempts")
    public void aUserExistsWithMaxLoginAttempts() throws Exception {
        String username = "user-login-attempts-max";
        createUserWithinAService(RandomIdGenerator.randomUuid(), username, "password");
        dbHelper.updateLoginCount(username, 10);
    }

    @State("a forgotten password entry exist")
    public void aForgottenPasswordEntryExist() throws Exception {
        String code = "existing-code";
        String existingUserExternalId = "7d19aff33f8948deb97ed16b2912dcd3";
        List<Map<String, Object>> userByName = dbHelper.findUserByExternalId(existingUserExternalId);
        dbHelper.add(ForgottenPassword.forgottenPassword(code, existingUserExternalId), (Integer) userByName.get(0).get("id"));
    }

    @State("a user and user admin exists in service with the given ids before a delete operation")
    public void aUserAndUserAdminExistBeforeADelete() throws Exception {

        String existingUserExternalId = "pact-delete-user-id";
        String existingUserRemoverExternalId = "pact-delete-remover-id";
        String existingServiceExternalId = "pact-delete-service-id";

        Service service = ServiceDbFixture.serviceDbFixture(dbHelper).withExternalId(existingServiceExternalId).insertService();
        Role role = RoleDbFixture.roleDbFixture(dbHelper).insertAdmin();

        UserDbFixture.userDbFixture(dbHelper).withExternalId(existingUserExternalId).withServiceRole(service, role.getId()).insertUser();
        UserDbFixture.userDbFixture(dbHelper).withExternalId(existingUserRemoverExternalId).withServiceRole(service, role.getId()).insertUser();
    }

    private static void createUserWithinAService(String externalId, String username, String password) throws Exception {
        createUserWithinAService(externalId, username, nextInt(), password);
    }

    @State({"a forgotten password does not exists",
            "a user does not exist",
            "a user exists",
            "no user exits with the given name",
            "a user exits with the given name",
            "a valid (non-expired) forgotten password entry does not exist",
            "default",
            "a user exist",
            "a user exists with a given username password",
            "a user not exists with a given username password",
            "a user not exists with a given username password",
            "no user exits with the given external id",
            "a user exits with the given external id"
    })
    public void noSetUp() {
    }

    private static void createUserWithinAService(String externalId, String username, int serviceId, String password) throws Exception {
        String gatewayAccount1 = randomNumeric(5);
        String gatewayAccount2 = randomNumeric(5);
        Role role = Role.role(randomInt(), "admin", "Administrator");
        roleDbFixture(dbHelper).insert(role,
                Permission.permission(randomInt(), "perm-1", "permission-1-description"),
                Permission.permission(randomInt(), "perm-2", "permission-2-description"),
                Permission.permission(randomInt(), "perm-3", "permission-3-description"));
        serviceDbFixture(dbHelper).withId(serviceId).withGatewayAccountIds(gatewayAccount1, gatewayAccount2).insertService();

        UserDbFixture.userDbFixture(dbHelper)
                .withExternalId(externalId)
                .withUsername(username)
                .withPassword(passwordHasher.hash(password))
                .withEmail("user-" + username + "@example.com")
                .withGatewayAccountIds(Arrays.asList(gatewayAccount1, gatewayAccount2))
                .withTelephoneNumber("45334534634")
                .withOtpKey("34f34")
                .withServiceRole(serviceId, role.getId())
                .insertUser();
    }
}
