package uk.gov.pay.adminusers.resources;

import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.entity.service.SupportedLanguage;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.persistence.dao.ServiceDaoTest.createServiceName;

public class ServiceResourceFindTest extends IntegrationTest {

    @Test
    public void shouldGet_existingServiceById_withoutNameVariants() throws Exception {

        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        databaseHelper.addService(service, randomInt().toString());
        givenSetup()
                .when()
                .accept(JSON)
                .get(format("/v1/api/services/%s", serviceExternalId))
                .then()
                .statusCode(200)
                .body("name", is("existing-name"))
                .body("service_name", hasKey("en"))
                .body("service_name.en", is("existing-name"))
                .body("$", not(hasKey("merchant_details")))
                .body("_links", hasSize(1))
                .body("_links[0].href", is("http://localhost:8080/v1/api/services/" + serviceExternalId))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("self"));
    }

    @Test
    public void shouldGetServiceById_withServiceNameVariantForCy() throws Exception {

        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        databaseHelper.addService(service, randomInt().toString());
        databaseHelper.addServiceName(createServiceName(SupportedLanguage.WELSH, "some-cy-name"), service.getId());
        givenSetup()
                .when()
                .accept(JSON)
                .get(format("/v1/api/services/%s", serviceExternalId))
                .then()
                .statusCode(200)
                .body("name", is("existing-name"))
                .body("service_name", hasKey("en"))
                .body("service_name.en", is("existing-name"))
                .body("service_name", hasKey("cy"))
                .body("service_name.cy", is("some-cy-name"))
                .body("$", not(hasKey("merchant_details")))
                .body("_links", hasSize(1))
                .body("_links[0].href", is("http://localhost:8080/v1/api/services/" + serviceExternalId))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("self"));
    }

    @Test
    public void shouldGetServiceById_withServiceNameVariantsForEn_andCy() throws Exception {

        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        databaseHelper.addService(service, randomInt().toString());
        databaseHelper.addServiceName(createServiceName(SupportedLanguage.ENGLISH, "existing-name"), service.getId());
        databaseHelper.addServiceName(createServiceName(SupportedLanguage.WELSH, "some-cy-name"), service.getId());
        givenSetup()
                .when()
                .accept(JSON)
                .get(format("/v1/api/services/%s", serviceExternalId))
                .then()
                .statusCode(200)
                .body("name", is("existing-name"))
                .body("service_name", hasKey("en"))
                .body("service_name.en", is("existing-name"))
                .body("service_name", hasKey("cy"))
                .body("service_name.cy", is("some-cy-name"))
                .body("$", not(hasKey("merchant_details")))
                .body("_links", hasSize(1))
                .body("_links[0].href", is("http://localhost:8080/v1/api/services/" + serviceExternalId))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("self"));
    }

    @Test
    public void shouldReturn404_whenGetServiceById_ifNotFound() throws Exception {

        givenSetup()
                .when()
                .accept(JSON)
                .get(format("/v1/api/services/%s", "non-existent-id"))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldFind_existingServiceByGatewayAccountId() throws Exception {

        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "existing-name");
        String gatewayAccountId = randomInt().toString();
        databaseHelper.addService(service, gatewayAccountId);

        givenSetup()
                .when()
                .accept(JSON)
                .get(format("/v1/api/services?gatewayAccountId=%s", gatewayAccountId))
                .then()
                .statusCode(200)
                .body("name", is("existing-name"))
                .body("service_name", hasKey("en"))
                .body("service_name.en", is("existing-name"))
                .body("gateway_account_ids[0]", is(gatewayAccountId));

    }

    @Test
    public void shouldReturn404_whenFindByGatewayAccountId_ifNotFound() {

        givenSetup()
                .when()
                .accept(JSON)
                .get(format("/v1/api/services?gatewayAccountId=%s", randomInt().toString()))
                .then()
                .statusCode(404);

    }
}
