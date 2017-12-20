package uk.gov.pay.adminusers.resources;

import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class ServiceResourceGetAllTest extends IntegrationTest {

    @Test
    public void shouldGetAllServices() throws Exception {

        String service1ExternalId = randomUuid();
        Service service1 = Service.from(randomInt(), service1ExternalId, "existing-name-1");
        databaseHelper.addService(service1, randomInt().toString());

        String service2ExternalId = randomUuid();
        Service service2 = Service.from(randomInt(), service2ExternalId, "existing-name-2");
        databaseHelper.addService(service2, randomInt().toString());

        givenSetup()
                .when()
                .accept(JSON)
                .get("/v1/api/services")
                .then()
                .statusCode(200)
                .body("get(0).name", equalTo("existing-name-1"))
                .body("get(1).name", equalTo("existing-name-2"));
    }
}
