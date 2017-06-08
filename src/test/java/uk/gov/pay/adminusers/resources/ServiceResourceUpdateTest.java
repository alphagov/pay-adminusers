package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class ServiceResourceUpdateTest extends IntegrationTest {

    //Update service name
    @Test
    public void shouldSuccess_whenReplaceServiceNameWithANewValue() throws Exception {

        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        databaseHelper.addService(service, randomInt().toString());

        Map<String, String> payload = ImmutableMap.of("op", "replace", "path", "name", "value", "updated-service-name");

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format("/v1/api/services/%s", serviceExternalId))
                .then()
                .statusCode(200)
                .body("name", is("updated-service-name"));

    }

    @Test
    public void shouldError404_ifServiceExternalIdDoesNotExist() throws Exception {
        Map<String, String> payload = ImmutableMap.of("op", "replace", "path", "name", "value", "new-service-name");

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format("/v1/api/services/%s", "non-existent-service-id"))
                .then()
                .statusCode(404);

    }

    @Test
    public void shouldError400_ifMandatoryFieldMissing() throws Exception {
        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        databaseHelper.addService(service, randomInt().toString());

        Map<String, String> payload = ImmutableMap.of("op", "replace", "path", "name");

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format("/v1/api/services/%s", "non-existent-service-id"))
                .then()
                .statusCode(400)
                .body("errors", hasSize(1))
                .body("errors", hasItems(format("Field [value] is required")));

    }

    //Assign gateway accounts
    @Test
    public void shouldSuccess_whenAddGatewayAccountIds() throws Exception {
        String existingGatewayAccountId = randomInt().toString();
        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        databaseHelper.addService(service, existingGatewayAccountId);

        String newGatewayAccountId = randomInt().toString();
        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("op", "add")
                .put("path", "gateway_account_ids")
                .put("value", asList(newGatewayAccountId))
                .build();

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format("/v1/api/services/%s", serviceExternalId))
                .then()
                .statusCode(200);

        List<String> gatewayAccounts = databaseHelper.findGatewayAccountsByService(serviceExternalId).stream()
                .map(row -> row.get("gateway_account_id").toString()).collect(toList());
        assertThat(gatewayAccounts.size(), is(2));
        assertThat(gatewayAccounts, hasItems(existingGatewayAccountId, newGatewayAccountId));

    }

    @Test
    public void shouldError409_whenAddGatewayAccountIds_ifGatewayAccountIdAlreadyUsedByAnotherService() throws Exception {
        String service1ExternalId = randomUuid();
        String service2GatewayAccountId = randomInt().toString();
        Service service1 = Service.from(randomInt(), service1ExternalId, "service-1-name");
        Service service2 = Service.from(randomInt(), randomUuid(), "service-2-name");
        databaseHelper
                .addService(service1, randomInt().toString())
                .addService(service2, service2GatewayAccountId);

        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put("op", "add")
                .put("path", "gateway_account_ids")
                .put("value", asList(service2GatewayAccountId))
                .build();

        givenSetup()
                .when()
                .accept(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format("/v1/api/services/%s", service1ExternalId))
                .then()
                .body("errors", hasSize(1))
                .body("errors", hasItems(format("One or more of the following gateway account ids has already assigned to another service: [%s]", service2GatewayAccountId)));

    }
}
