package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardClientExtension;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.infra.AppWithPostgresExtension;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
public class IntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);

    /* default */ static final String USERS_RESOURCE_URL = "/v1/api/users";
    /* default */ static final String FIND_RESOURCE_URL = "/v1/api/users/find";
    /* default */ static final String USER_RESOURCE_URL = "/v1/api/users/%s";
    /* default */ static final String USERS_AUTHENTICATE_URL = "/v1/api/users/authenticate";
    /* default */ static final String USER_2FA_URL = "/v1/api/users/%s/second-factor";
    /* default */ static final String USER_SERVICE_RESOURCE = "/v1/api/users/%s/services/%s";

    /* default */ static final String INVITES_RESOURCE_URL = "/v1/api/invites";
    /* default */ static final String INVITES_SEND_OTP_RESOURCE_URL = "/v1/api/invites/%s/send-otp";
    /* default */ static final String INVITES_REPROVISION_OTP_RESOURCE_URL = "/v1/api/invites/%s/reprovision-otp";
    /* default */ static final String INVITES_VALIDATE_OTP_RESOURCE_URL = "/v2/api/invites/otp/validate";
    /* default */ static final String SERVICES_RESOURCE = "/v1/api/services";
    /* default */ static final String SERVICE_RESOURCE = SERVICES_RESOURCE + "/%s";

    @Deprecated
    /* default */ static final String SERVICE_INVITES_RESOURCE_URL = "/v1/api/services/%d/invites";

    public static final DropwizardClientExtension NOTIFY;

    @RegisterExtension
    public static final AppWithPostgresExtension APP;

    protected DatabaseTestHelper databaseHelper;
    protected static ObjectMapper mapper = new ObjectMapper();

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
        NOTIFY = new DropwizardClientExtension(new NotifyResource());
        try {
            // starts dropwizard application. This is required as we don't use DropwizardExtensionsSupport (which actually starts application)
            // due to static initialisation of client extension which is needed for DropwizardAppWithPostgresExtension further
            NOTIFY.before();
        } catch (Throwable throwable) {
            LOGGER.error("Exception starting client extension for NotifyResource - {}", throwable.getMessage());
            throw new RuntimeException(throwable);
        }
        APP = new AppWithPostgresExtension(
                ConfigOverride.config("notify.notificationBaseURL", () -> NOTIFY.baseUri().toString()));
    }

    @BeforeEach
    public void initialise() {
        databaseHelper = APP.getDatabaseTestHelper();
        databaseHelper.truncateAllData();
    }

    protected RequestSpecification givenSetup() {
        return given().port(APP.getLocalPort())
                .contentType(JSON);
    }
    
    protected static Injector getInjector() {
        return APP.getInjector();
    }
}
