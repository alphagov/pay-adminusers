package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import uk.gov.pay.adminusers.app.util.RandomIdGenerator;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;
import uk.gov.pay.adminusers.infra.DropwizardAppWithPostgresRule;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.service.PasswordHasher;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.*;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

@RunWith(PactRunner.class)
@Provider("AdminUsers")
@PactFolder("pacts")
public class UsersApiTest {

    @ClassRule
    public static DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    private static final PasswordHasher passwordHasher = new PasswordHasher();
    private static DatabaseTestHelper dbHelper;

    @BeforeClass
    public static void setUpService() throws Exception {
        target = new HttpTarget(app.getLocalPort());
        dbHelper = app.getDatabaseTestHelper();
        int serviceId = 12345;
        createUserWithinAService("7d19aff33f8948deb97ed16b2912dcd3", "existing-user", serviceId, "password");
    }

    @TestTarget
    public static Target target;

    @State("a valid forgotten password entry and a related user exists")
    public void aUserExistsWithAForgottenPasswordRequest() throws Exception {
        String code = "avalidforgottenpasswordtoken";
        String username = randomAlphabetic(20);
        createUserWithinAService(RandomIdGenerator.randomUuid(), username, "password");
        List<Map<String, Object>> userByUsername = dbHelper.findUserByUsername(username);
        dbHelper.add(ForgottenPassword.forgottenPassword(code, username), (Integer) userByUsername.get(0).get("id"));
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
        String username = "existing-user";
        List<Map<String, Object>> userByName = dbHelper.findUserByUsername(username);
        dbHelper.add(ForgottenPassword.forgottenPassword(code, username), (Integer) userByName.get(0).get("id"));
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
}
