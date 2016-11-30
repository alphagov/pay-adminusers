package uk.gov.pay.adminusers.resources;

import com.jayway.restassured.specification.RequestSpecification;
import org.junit.Before;
import org.junit.ClassRule;
import uk.gov.pay.adminusers.infra.DropwizardAppWithPostgresRule;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;

public class IntegrationTest {

    @ClassRule
    public static DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    protected DatabaseTestHelper databaseTestHelper;

    @Before
    public void setUp() {
        databaseTestHelper = app.getDatabaseTestHelper();
    }

    protected RequestSpecification givenSetup() {
        return given().port(app.getLocalPort())
                .contentType(JSON);
    }
}
