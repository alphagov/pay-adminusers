package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.ValidatableResponse;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

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

        ValidatableResponse validatableResponse = givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .post("/v1/api/services")
                .then()
                .statusCode(201);

        validatableResponse
                .body("name", is("System Generated"))
                .body("external_id", notNullValue());

        String serviceExternalId = validatableResponse.extract().jsonPath().getString("external_id");

        List<Map<String, Object>> gatewayAccountsForService = databaseHelper.findGatewayAccountsByService(serviceExternalId);
        List<String> gatewayAccountIds = gatewayAccountsForService.stream()
                .map(gaEntry -> gaEntry.get("gateway_account_id").toString()).collect(toList());

        assertThat(gatewayAccountIds.size(), is(2));
        assertThat(gatewayAccountIds, hasItems("1","2"));
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
        databaseHelper.addService(Service.from(serviceId, newId(), "test-service-1"), gatewayAccountId);

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
