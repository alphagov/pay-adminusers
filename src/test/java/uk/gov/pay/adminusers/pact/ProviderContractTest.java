package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import uk.gov.pay.adminusers.app.util.RandomIdGenerator;
import uk.gov.pay.adminusers.fixtures.RoleDbFixture;
import uk.gov.pay.adminusers.fixtures.ServiceDbFixture;
import uk.gov.pay.adminusers.infra.DropwizardAppWithPostgresRule;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.GoLiveStage;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.service.PasswordHasher;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

@RunWith(PactRunner.class)
@Provider("adminusers")
@PactBroker(scheme = "https", host = "pact-broker-test.cloudapps.digital", tags = {"${PACT_CONSUMER_TAG}", "test", "staging", "production"},
        authentication = @PactBrokerAuth(username = "${PACT_BROKER_USERNAME}", password = "${PACT_BROKER_PASSWORD}"))
//@PactFolder("pacts")
public class ProviderContractTest {

    @ClassRule
    public static DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @TestTarget
    public static Target target;

    private static final PasswordHasher passwordHasher = new PasswordHasher();
    private static DatabaseTestHelper dbHelper;

    @BeforeClass
    public static void setUpService() {
        target = new HttpTarget(app.getLocalPort());
        dbHelper = app.getDatabaseTestHelper();
        // make sure we create services(including gateway account ids) before users
        serviceDbFixture(dbHelper).withGatewayAccountIds("268").insertService();
    }

    @Before
    public void resetDatabase() {
        dbHelper.truncateAllData();
    }

    @State("a valid forgotten password entry and a related user exists")
    public void aUserExistsWithAForgottenPasswordRequest() {
        String code = "avalidforgottenpasswordtoken";
        String userExternalId = RandomIdGenerator.randomUuid();
        createUserWithinAService(userExternalId, RandomIdGenerator.randomUuid(), "password", "cp5wa");
        List<Map<String, Object>> userByExternalId = dbHelper.findUserByExternalId(userExternalId);
        dbHelper.add(ForgottenPassword.forgottenPassword(code, userExternalId), (Integer) userByExternalId.get(0).get("id"));
    }

    @State("a user exists with max login attempts")
    public void aUserExistsWithMaxLoginAttempts() {
        String username = "user-login-attempts-max";
        createUserWithinAService(RandomIdGenerator.randomUuid(), username, "password", "cp5wa");
        dbHelper.updateLoginCount(username, 10);
    }

    @State("a forgotten password entry exist")
    public void aForgottenPasswordEntryExist() {
        String code = "existing-code";
        String existingUserExternalId = "7d19aff33f8948deb97ed16b2912dcd3";
        createUserWithinAService(existingUserExternalId, "forgotten-password-user", "password", "cp5wa");
        List<Map<String, Object>> userByName = dbHelper.findUserByExternalId(existingUserExternalId);
        dbHelper.add(ForgottenPassword.forgottenPassword(code, existingUserExternalId), (Integer) userByName.get(0).get("id"));
    }

    @State("a user and user admin exists in service with the given ids before a delete operation")
    public void aUserAndUserAdminExistBeforeADelete() {

        String existingUserExternalId = "pact-delete-user-id";
        String existingUserRemoverExternalId = "pact-delete-remover-id";
        String existingServiceExternalId = "pact-delete-service-id";

        Service service = ServiceDbFixture.serviceDbFixture(dbHelper).withExternalId(existingServiceExternalId).insertService();
        Role role = RoleDbFixture.roleDbFixture(dbHelper).insertAdmin();

        String username1 = randomUuid();
        String email1 = username1 + "@example.com";
        userDbFixture(dbHelper).withExternalId(existingUserExternalId).withServiceRole(service, role.getId()).withUsername(username1).withEmail(email1).insertUser();
        String username2 = randomUuid();
        String email2 = username2 + "@example.com";
        userDbFixture(dbHelper).withExternalId(existingUserRemoverExternalId).withServiceRole(service, role.getId()).withUsername(username2).withEmail(email2).insertUser();
    }

    @State("a user exists but not the remover before a delete operation")
    public void aUserExistButRemoverBeforeADelete() {

        String existingUserExternalId = "pact-user-no-remover-test";
        String existingServiceExternalId = "pact-service-no-remover-test";

        Service service = ServiceDbFixture.serviceDbFixture(dbHelper).withExternalId(existingServiceExternalId).insertService();
        Role role = RoleDbFixture.roleDbFixture(dbHelper).insertAdmin();

        String username = randomUuid();
        String email = username + "@example.com";
        userDbFixture(dbHelper).withExternalId(existingUserExternalId).withServiceRole(service, role.getId()).withUsername(username).withEmail(email).insertUser();
    }

    @State({"a forgotten password does not exists",
            "a user does not exist",
            "no user exits with the given name",
            "a user exits with the given name",
            "a valid (non-expired) forgotten password entry does not exist",
            "default",
            "a user exist",
            "a user exists with a given username password",
            "a user not exists with a given username password",
            "a user not exists with a given username password",
            "no user exists with the given external id"
    })
    public void noSetUp() {
    }

    @State({"a user exists with the given external id 7d19aff33f8948deb97ed16b2912dcd3",
            "a user exists",
            "a user exists with username existing-user",
            "a user exists with username existing-user and password password",
            "a user exists with role for service with id cp5wa",
            "a user exists with external id 7d19aff33f8948deb97ed16b2912dcd3 with admin role for service with id cp5wa"})
    public void aUserExistsWithGivenExternalId() {
        createUserWithinAService("7d19aff33f8948deb97ed16b2912dcd3", "existing-user", "password", "cp5wa");
    }
    
    @State("a user exists external id 7d19aff33f8948deb97ed16b2912dcd3 and a service exists with external id cp5wa")
    public void aUserExistsNotAssignedToService() {
        serviceDbFixture(dbHelper)
                .withExternalId("cp5wa")
                .insertService();

        userDbFixture(dbHelper)
                .withExternalId("7d19aff33f8948deb97ed16b2912dcd3")
                .insertUser();
    }

    @State({"a service exists with external id cp5wa and billing address collection enabled",
            "a service exists with external id cp5wa",
            "a service exists with external id cp5wa with gateway account with id 111"})
    public void aServiceExists() {
        serviceDbFixture(dbHelper)
                .withExternalId("cp5wa")
                .withGatewayAccountIds("111")
                .insertService();
    }
    
    @State({"a service exists with custom branding and a gateway account with id 111"})
    public void aServiceExistsWithCustomBranding() {
        serviceDbFixture(dbHelper)
                .withGatewayAccountIds("111")
                .withCustomBranding("https://example.org/mycss", "https://example.org/myimage")
                .insertService();
    }

    @State("a service exists with external id rtglNotStarted and go live stage equals to NOT_STARTED")
    public void aServiceExistsWithNotStartedGoLiveStage() {
        serviceDbFixture(dbHelper)
                .withExternalId("rtglNotStarted")
                .withGoLiveStage(GoLiveStage.NOT_STARTED)
                .insertService();
    }
    
    @State("a service exists with external id cp5wa with multiple admin users")
    public void aServiceExistsWithMultipleAdmins() {
        Service service = serviceDbFixture(dbHelper)
                .withExternalId("cp5wa")
                .insertService();

        Role role = createRole();
        createUserWithRoleForService("7d19aff33f8948deb97ed16b2912dcd3", "existing-user", "password", role, service);
        createUserWithRoleForService("admin-2-id", "admin-2", "password", role, service);
    }

    private static void createUserWithinAService(String externalId, String username, String password, String serviceExternalId) {
        String gatewayAccount1 = randomNumeric(5);
        String gatewayAccount2 = randomNumeric(5);
        Service service = serviceDbFixture(dbHelper)
                .withExternalId(serviceExternalId)
                .withGatewayAccountIds(gatewayAccount1, gatewayAccount2)
                .insertService();

        Role role = createRole();
        createUserWithRoleForService(externalId, username, password, role, service);
    }
    
    private static Role createRole() {
        Role role = Role.role(2,"admin", "Administrator");
        return roleDbFixture(dbHelper).insert(role,
                Permission.permission(randomInt(), "perm-1", "permission-1-description"),
                Permission.permission(randomInt(), "perm-2", "permission-2-description"),
                Permission.permission(randomInt(), "perm-3", "permission-3-description"));
    }
    
    private static void createUserWithRoleForService(String externalId, String username, String password, Role role, Service service)
    {
        userDbFixture(dbHelper)
                .withExternalId(externalId)
                .withUsername(username)
                .withPassword(passwordHasher.hash(password))
                .withEmail("user-" + username + "@example.com")
                .withTelephoneNumber("45334534634")
                .withOtpKey("34f34")
                .withProvisionalOtpKey("94423")
                .withServiceRole(service.getId(), role.getId())
                .insertUser();
    }
}
