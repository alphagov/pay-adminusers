package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.specification.RequestSpecification;
import org.junit.Before;
import org.junit.ClassRule;
import uk.gov.pay.adminusers.infra.DropwizardAppWithPostgresRule;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;

public class IntegrationTest {

    static final String USERS_RESOURCE_URL = "/v1/api/users";
    static final String FIND_RESOURCE_URL = "/v1/api/users/find";
    static final String USER_RESOURCE_URL = "/v1/api/users/%s";
    static final String USERS_AUTHENTICATE_URL = "/v1/api/users/authenticate";
    static final String USER_2FA_URL = "/v1/api/users/%s/second-factor";
    static final String USER_SERVICES_RESOURCE = USER_RESOURCE_URL + "/services";
    static final String USER_SERVICE_RESOURCE = USER_RESOURCE_URL + "/services/%s";

    static final String INVITES_RESOURCE_URL = "/v1/api/invites";
    static final String INVITES_GENERATE_OTP_RESOURCE_URL = "/v1/api/invites/%s/otp/generate";
    static final String INVITES_RESEND_OTP_RESOURCE_URL = "/v1/api/invites/otp/resend";
    static final String INVITES_VALIDATE_OTP_RESOURCE_URL = "/v1/api/invites/otp/validate";

    static final String SERVICE_INVITES_VALIDATE_OTP_RESOURCE_URL = "/v1/api/invites/otp/validate/service";
    static final String SERVICES_RESOURCE = "/v1/api/services";
    static final String SERVICE_RESOURCE = SERVICES_RESOURCE + "/%s";

    @Deprecated
    static final String SERVICE_INVITES_RESOURCE_URL = "/v1/api/services/%d/invites";
    static final String INVITE_USER_RESOURCE_URL = "/v1/api/invites/user";

    @ClassRule
    public static final DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    protected DatabaseTestHelper databaseHelper;
    protected ObjectMapper mapper;

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
