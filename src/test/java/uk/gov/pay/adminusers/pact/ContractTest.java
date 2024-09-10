package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import uk.gov.pay.adminusers.infra.AppWithPostgresAndSqsRule;
import uk.gov.pay.adminusers.model.GoLiveStage;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.service.PasswordHasher;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.ForgottenPasswordDbFixture.aForgottenPasswordDbFixture;
import static uk.gov.pay.adminusers.fixtures.InviteDbFixture.inviteDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public abstract class ContractTest {

    @ClassRule
    public static AppWithPostgresAndSqsRule app = new AppWithPostgresAndSqsRule();

    @TestTarget
    public static Target target;

    private static final PasswordHasher PASSWORD_HASHER = new PasswordHasher();
    private static DatabaseTestHelper dbHelper;
    private static Role adminRole;

    @BeforeClass
    public static void setUpService() {
        target = new HttpTarget(app.getLocalPort());
        dbHelper = app.getDatabaseTestHelper();
        // make sure we create services(including gateway account ids) before users
        serviceDbFixture(dbHelper).withGatewayAccountIds("268").insertService();
        adminRole = app.getInjector().getInstance(RoleDao.class).findByRoleName(RoleName.ADMIN).get().toRole();
    }

    @Before
    public void resetDatabase() {
        dbHelper.truncateAllData();
    }

    @State("a valid forgotten password entry and a related user exists")
    public void aUserExistsWithAForgottenPasswordRequest() {
        String code = "avalidforgottenpasswordtoken";
        String userExternalId = randomUuid();
        createUserWithinAService(userExternalId, randomUuid() + "@example.com", "password", "cp5wa");
        List<Map<String, Object>> userByExternalId = dbHelper.findUserByExternalId(userExternalId);

        aForgottenPasswordDbFixture()
                .withDatabaseTestHelper(dbHelper)
                .withUserId((Integer) userByExternalId.get(0).get("id"))
                .withCode(code)
                .insert();
    }

    @State("a user exists with max login attempts")
    public void aUserExistsWithMaxLoginAttempts() {
        String email = "user-login-attempts-max@example.com";
        createUserWithinAService(randomUuid(), email, "password", "cp5wa");
        dbHelper.updateLoginCount(email, 10);
    }

    @State("a forgotten password entry exist")
    public void aForgottenPasswordEntryExist() {
        String code = "existing-code";
        String existingUserExternalId = "7d19aff33f8948deb97ed16b2912dcd3";
        createUserWithinAService(existingUserExternalId, "forgotten-password-user@example.com", "password", "cp5wa");
        List<Map<String, Object>> userByName = dbHelper.findUserByExternalId(existingUserExternalId);

        aForgottenPasswordDbFixture()
                .withDatabaseTestHelper(dbHelper)
                .withUserId((Integer) userByName.get(0).get("id"))
                .withCode(code)
                .insert();
    }

    @State("a user and user admin exists in service with the given ids before a delete operation")
    public void aUserAndUserAdminExistBeforeADelete() {

        String existingUserExternalId = "pact-delete-user-id";
        String existingUserRemoverExternalId = "pact-delete-remover-id";
        String existingServiceExternalId = "pact-delete-service-id";

        Service service = serviceDbFixture(dbHelper).withExternalId(existingServiceExternalId).insertService();

        String email1 = randomUuid() + "@example.com";
        userDbFixture(dbHelper).withExternalId(existingUserExternalId).withServiceRole(service, adminRole).withEmail(email1).insertUser();
        String email2 = randomUuid() + "@example.com";
        userDbFixture(dbHelper).withExternalId(existingUserRemoverExternalId).withServiceRole(service, adminRole).withEmail(email2).insertUser();
    }

    @State("a user exists but not the remover before a delete operation")
    public void aUserExistButRemoverBeforeADelete() {

        String existingUserExternalId = "pact-user-no-remover-test";
        String existingServiceExternalId = "pact-service-no-remover-test";

        Service service = serviceDbFixture(dbHelper).withExternalId(existingServiceExternalId).insertService();

        String email = randomUuid() + "@example.com";
        userDbFixture(dbHelper).withExternalId(existingUserExternalId).withServiceRole(service, adminRole).withEmail(email).insertUser();
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
            "a user exists with email existing-user@example.com",
            "a user exists with email existing-user@example.com and password password",
            "a user exists with role for service with id cp5wa",
            "a user exists with external id 7d19aff33f8948deb97ed16b2912dcd3 with admin role for service with id cp5wa"})
    public void aUserExistsWithGivenExternalId() {
        createUserWithinAService("7d19aff33f8948deb97ed16b2912dcd3", "existing-user@example.com", "password", "cp5wa");
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

        createUserWithRoleForService("7d19aff33f8948deb97ed16b2912dcd3", "existing-user@example.com", "password", adminRole, service);
        createUserWithRoleForService("admin-2-id", "admin-2@example.com", "password", adminRole, service);
    }

    @State("a valid self-signup invite exists with invite code an-invite-code")
    public void aSelfSignupInviteExists() {
        inviteDbFixture(dbHelper)
                .withCode("an-invite-code")
                .withPassword("a-password")
                .insertSelfSignupInvite(adminRole);
    }

    @State("a valid invite to add a user to a service exists with invite code an-invite-code")
    public void anAddUserToServiceInviteExists() {
        inviteDbFixture(dbHelper)
                .withCode("an-invite-code")
                .withPassword("a-password")
                .withTelephoneNumber("+441134960000")
                .insertInviteToAddUserToService(adminRole);
    }

    @State("an invite to add an existing user to a service exists with invite code an-invite-code")
    public void aUserAndInviteToAddUserToServiceExist() {
        String email = "foo@example.com";
        userDbFixture(dbHelper)
                .withEmail(email)
                .insertUser();
        inviteDbFixture(dbHelper)
                .withEmail(email)
                .withCode("an-invite-code")
                .insertInviteToAddUserToService(adminRole);
    }

    private void createUserWithinAService(String externalId, String email, String password, String serviceExternalId) {
        String gatewayAccount1 = randomNumeric(5);
        String gatewayAccount2 = randomNumeric(5);
        Service service = serviceDbFixture(dbHelper)
                .withExternalId(serviceExternalId)
                .withGatewayAccountIds(gatewayAccount1, gatewayAccount2)
                .insertService();

        createUserWithRoleForService(externalId, email, password, adminRole, service);
    }

    private void createUserWithRoleForService(String externalId, String email, String password, Role role, Service service) {
        userDbFixture(dbHelper)
                .withExternalId(externalId)
                .withPassword(PASSWORD_HASHER.hash(password))
                .withEmail(email)
                .withTelephoneNumber("45334534634")
                .withOtpKey("34f34")
                .withProvisionalOtpKey("94423")
                .withServiceRole(service.getId(), role)
                .insertUser();
    }
}
