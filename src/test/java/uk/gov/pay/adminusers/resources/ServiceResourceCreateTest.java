package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;

import java.util.Map;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class ServiceResourceCreateTest extends IntegrationTest {

    @Test
    public void shouldSuccess_whenCreateAServiceWithName() throws Exception{

        Map<String, String> payload = ImmutableMap.of("name", "some service name");

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .post("/v1/api/services")
                .then()
                .statusCode(201)
                .body("name", is("some service name"))
                .body("external_id", notNullValue());

    }

    @Test
    public void shouldSuccess_whenCreateAServiceWithValidGatewayAccounts() throws Exception{

        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("gateway_account_ids", new String[]{"1", "2"})
                .build();

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .post("/v1/api/services")
                .then()
                .statusCode(201)
                .body("name", is("System Generated"))
                .body("external_id", notNullValue())
                .body("gateway_account_ids", hasSize(2))
                .body("gateway_account_ids", containsInAnyOrder("1","2"));

    }

    @Test
    public void shouldError400_whenCreateAServiceWithInvalidGatewayAccounts() throws Exception{

        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("name", "some service name")
                .put("gateway_account_ids", new String[]{"blah", "blahblah"})
                .build();

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .post("/v1/api/services")
                .then()
                .statusCode(400)
                .body("errors", hasSize(1))
                .body("errors", hasItems("Field [gateway_account_ids] must contain numeric values"));

    }

    @Test
    public void shouldError409_whenGatewayAccountsAreAlreadyAssignedToAService() throws Exception{
        int serviceId = randomInt();
        String gatewayAccountId = String.valueOf(randomInt());
        databaseHelper.addService(Service.from(serviceId, randomUuid(), "test-service-1"), gatewayAccountId);

        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("name", "some service name")
                .put("gateway_account_ids", new String[]{gatewayAccountId})
                .build();

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .post("/v1/api/services")
                .then()
                .statusCode(409)
                .body("errors", hasSize(1))
                .body("errors", hasItems(format("One or more of the following gateway account ids has already assigned to another service: [%s]", gatewayAccountId)));

    }


}
