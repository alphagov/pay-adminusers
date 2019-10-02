package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardClientRule;
import io.restassured.specification.RequestSpecification;
import org.junit.Before;
import org.junit.ClassRule;
import uk.gov.pay.adminusers.infra.DropwizardAppWithPostgresRule;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class IntegrationTest {

    /* default */ static final String USERS_RESOURCE_URL = "/v1/api/users";
    /* default */ static final String FIND_RESOURCE_URL = "/v1/api/users/find";
    /* default */ static final String USER_RESOURCE_URL = "/v1/api/users/%s";
    /* default */ static final String USERS_AUTHENTICATE_URL = "/v1/api/users/authenticate";
    /* default */ static final String USER_2FA_URL = "/v1/api/users/%s/second-factor";
    /* default */ static final String USER_SERVICES_RESOURCE = USER_RESOURCE_URL + "/services";
    /* default */ static final String USER_SERVICE_RESOURCE = USER_RESOURCE_URL + "/services/%s";

    /* default */ static final String INVITES_RESOURCE_URL = "/v1/api/invites";
    /* default */ static final String INVITES_GENERATE_OTP_RESOURCE_URL = "/v1/api/invites/%s/otp/generate";
    /* default */ static final String INVITES_RESEND_OTP_RESOURCE_URL = "/v1/api/invites/otp/resend";
    /* default */ static final String INVITES_VALIDATE_OTP_RESOURCE_URL = "/v1/api/invites/otp/validate";

    /* default */ static final String SERVICE_INVITES_VALIDATE_OTP_RESOURCE_URL = "/v1/api/invites/otp/validate/service";
    /* default */ static final String SERVICES_RESOURCE = "/v1/api/services";
    /* default */ static final String SERVICE_RESOURCE = SERVICES_RESOURCE + "/%s";

    @Deprecated
    /* default */ static final String SERVICE_INVITES_RESOURCE_URL = "/v1/api/services/%d/invites";
    /* default */ static final String INVITE_USER_RESOURCE_URL = "/v1/api/invites/user";

    @ClassRule
    public static final DropwizardAppWithPostgresRule app;

    @ClassRule
    public static final DropwizardClientRule notify;

    protected DatabaseTestHelper databaseHelper;
    protected ObjectMapper mapper;

    @Path("/v2/notifications")
    public static class NotifyResource {
        @Path("/email")
        @POST
        @Produces(APPLICATION_JSON)
        @Consumes(APPLICATION_JSON)
        public Response sendEmail() {
            String response = "{\"id\":\"f1356064-37b6-499c-bec9-a167646255ff\", \"content\": {\"subject\":\"hello\", \"body\":\"bla\"}, \"template\": {\"id\":\"f1356064-37b6-499c-bec9-a167646255ff\", \"version\":0, \"uri\":\"lol\"}}";
            return Response.status(201).entity(response).type(APPLICATION_JSON).build();
        }
    }

    static {
        notify = new DropwizardClientRule(new NotifyResource());
        app = new DropwizardAppWithPostgresRule(
                ConfigOverride.config("notify.notificationBaseURL", () -> notify.baseUri().toString())
        );
    }

    @Before
    public void setUp() {
        databaseHelper = app.getDatabaseTestHelper();
        mapper = new ObjectMapper();
    }

    protected RequestSpecification givenSetup() {
        return given().port(app.getLocalPort())
                .contentType(JSON);
    }
}
