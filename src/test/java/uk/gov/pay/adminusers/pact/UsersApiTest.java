package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.VerificationReports;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth;
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

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

@RunWith(PayPactRunner.class)
@Provider("adminusers")
@PactBroker(protocol = "https", host = "${PACT_BROKER_HOST}", port = "443", tags = {"latest"}, authentication = @PactBrokerAuth(username = "${PACT_BROKER_USERNAME}", password = "${PACT_BROKER_PASSWORD"))
public class UsersApiTest {

    private static final PasswordHasher passwordHasher = new PasswordHasher();
    @ClassRule
    public static DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();
    @TestTarget
    public static Target target;
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

    private static void createUserWithinAService(String externalId, String username, String password) throws Exception {
        createUserWithinAService(externalId, username, nextInt(), password);
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

    @PactVerifyProvider("a valid forgotten password entry and a related user exists")
    public void aUserExistsWithAForgottenPasswordRequest() throws Exception {
        String code = "avalidforgottenpasswordtoken";
        String userExternalId = RandomIdGenerator.randomUuid();
        createUserWithinAService(userExternalId, RandomIdGenerator.randomUuid(), "password");
        List<Map<String, Object>> userByExternalId = dbHelper.findUserByExternalId(userExternalId);
        dbHelper.add(ForgottenPassword.forgottenPassword(code, userExternalId), (Integer) userByExternalId.get(0).get("id"));
    }

    @PactVerifyProvider("a user exists with max login attempts")
    public void aUserExistsWithMaxLoginAttempts() throws Exception {
        String username = "user-login-attempts-max";
        createUserWithinAService(RandomIdGenerator.randomUuid(), username, "password");
        dbHelper.updateLoginCount(username, 10);
    }

    @PactVerifyProvider("a forgotten password entry exist")
    public void aForgottenPasswordEntryExist() throws Exception {
        String code = "existing-code";
        String existingUserExternalId = "7d19aff33f8948deb97ed16b2912dcd3";
        List<Map<String, Object>> userByName = dbHelper.findUserByExternalId(existingUserExternalId);
        dbHelper.add(ForgottenPassword.forgottenPassword(code, existingUserExternalId), (Integer) userByName.get(0).get("id"));
    }

    @PactVerifyProvider("a user and user admin exists in service with the given ids before a delete operation")
    public void aUserAndUserAdminExistBeforeADelete() throws Exception {

        String existingUserExternalId = "pact-delete-user-id";
        String existingUserRemoverExternalId = "pact-delete-remover-id";
        String existingServiceExternalId = "pact-delete-service-id";

        Service service = ServiceDbFixture.serviceDbFixture(dbHelper).withExternalId(existingServiceExternalId).insertService();
        Role role = RoleDbFixture.roleDbFixture(dbHelper).insertAdmin();

        String username1 = randomUuid();
        String email1 = username1 + "@example.com";
        UserDbFixture.userDbFixture(dbHelper).withExternalId(existingUserExternalId).withServiceRole(service, role.getId()).withUsername(username1).withEmail(email1).insertUser();
        String username2 = randomUuid();
        String email2 = username2 + "@example.com";
        UserDbFixture.userDbFixture(dbHelper).withExternalId(existingUserRemoverExternalId).withServiceRole(service, role.getId()).withUsername(username2).withEmail(email2).insertUser();
    }

    @PactVerifyProvider("a user exists but not the remover before a delete operation")
    public void aUserExistButRemoverBeforeADelete() throws Exception {

        String existingUserExternalId = "pact-user-no-remover-test";
        String existingServiceExternalId = "pact-service-no-remover-test";

        Service service = ServiceDbFixture.serviceDbFixture(dbHelper).withExternalId(existingServiceExternalId).insertService();
        Role role = RoleDbFixture.roleDbFixture(dbHelper).insertAdmin();

        String username = randomUuid();
        String email = username + "@example.com";
        UserDbFixture.userDbFixture(dbHelper).withExternalId(existingUserExternalId).withServiceRole(service, role.getId()).withUsername(username).withEmail(email).insertUser();
    }

    @State("the given external id all refer to existing users")
    public void d (){

    }

    @State("a user exists with the given external id")
    public void e (){ }

    @State("no users exits with the given external id")
    public void f (){ }

    @State("no user exists with the given external id")
    public void g (){ }

/*
    @PactVerifyProvider({"a forgotten password does not exists",
            "no user exits with the given name",
            "a user exits with the given name",
            "a valid (non-expired) forgotten password entry does not exist",
            "default",
            "a user exists with a given username password",
            "a user not exists with a given username password",
            "a user not exists with a given username password",
            ,
    })
    public void noSetUp() {
    }*/
}
