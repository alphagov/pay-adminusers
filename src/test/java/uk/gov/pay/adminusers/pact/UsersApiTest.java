package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import uk.gov.pay.adminusers.infra.DropwizardAppWithPostgresRule;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

@RunWith(PactRunner.class)
@Provider("AdminUsers")
@PactFolder("pacts")
public class UsersApiTest {

    @ClassRule
    public static DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    private static final ObjectMapper mapper = new ObjectMapper();
    private static DatabaseTestHelper dbHelper;

    @BeforeClass
    public static void setUpService() throws Exception {
        target = new HttpTarget(app.getLocalPort());
        dbHelper = app.getDatabaseTestHelper();
        int serviceId = 12345;
        createUserWithinAService("existing-user", serviceId, "password");
    }

    @TestTarget
    public static Target target;

    @State("a valid forgotten password entry and a related user exists")
    public void aUserExistsWithAForgottenPasswordRequest() throws Exception {
        String code = "avalidforgottenpasswordtoken";
        String username = RandomStringUtils.randomAlphabetic(20);
        createUserWithinAService(username, "password");
        List<Map<String, Object>> userByName = dbHelper.findUserByName(username);
        dbHelper.add(ForgottenPassword.forgottenPassword(code, username), (Integer) userByName.get(0).get("id"));
    }

    @State("a user exists with max login attempts")
    public void aUserExistsWithMaxLoginAttempts() throws Exception {
        String username = "user-login-attempts-max";
        createUserWithinAService(username, "password");
        dbHelper.updateLoginCount(username, 10);
    }

    @State("a forgotten password entry exist")
    public void aForgottenPasswordEntryExist() throws Exception {
        String code = "existing-code";
        String username = "existing-user";
        List<Map<String, Object>> userByName = dbHelper.findUserByName(username);
        dbHelper.add(ForgottenPassword.forgottenPassword(code, username), (Integer) userByName.get(0).get("id"));
    }

    private static void createUserWithinAService(String username, String password) throws Exception {
        createUserWithinAService(username, nextInt(), password);
    }

    private static void createUserWithinAService(String username, int serviceId, String password) throws Exception {

        roleDbFixture(dbHelper).withName("admin").insertRole();
        serviceDbFixture(dbHelper).withId(serviceId).withGatewayAccountIds("1", "2").insertService();

        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("password", password)
                .put("email", "user-" + username + "@example.com")
                .put("gateway_account_ids", new String[]{"1", "2"})
                .put("telephone_number", "45334534634")
                .put("otp_key", "34f34")
                .put("role_name", "admin")
                .build();

        given().port(app.getLocalPort())
                .contentType(JSON)
                .when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post("/v1/api/users")
                .then()
                .statusCode(201);
    }
}
