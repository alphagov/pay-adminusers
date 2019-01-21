package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.GovUkPayAgreementDbFixture;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.utils.DateTimeUtils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class ServiceResourceGovUkPayAgreementResourceTest extends IntegrationTest {

    private Service service;
    private User user;

    @Before
    public void setup() {
        service = serviceDbFixture(databaseHelper).insertService();
        user = userDbFixture(databaseHelper).insertUser();
    }
    
    @Test
    public void shouldCreateGovUkPayAgreement() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("user_external_id", user.getExternalId()));
        givenSetup()
                .when()
                .accept(JSON)
                .body(payload)
                .post(format("/v1/api/services/%s/govuk-pay-agreement", service.getExternalId()))
                .then()
                .statusCode(200);
    }

    @Test
    public void shouldReturn_404_whenServiceNotFound() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("user_external_id", user.getExternalId()));
        givenSetup()
                .when()
                .accept(JSON)
                .body(payload)
                .post(format("/v1/api/services/%s/govuk-pay-agreement", "abcde1234"))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldReturn_404_whenUserNotFound() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("user_external_id", "abcde1234"));
        givenSetup()
                .when()
                .accept(JSON)
                .body(payload)
                .post(format("/v1/api/services/%s/govuk-pay-agreement", service.getExternalId()))
                .then()
                .statusCode(404);
    }
    
    @Test
    public void shouldReturn_400_whenUserExternalIdIsNotAString() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("user_external_id", 100));
        givenSetup()
                .when()
                .accept(JSON)
                .body(payload)
                .post(format("/v1/api/services/%s/govuk-pay-agreement", service.getExternalId()))
                .then()
                .statusCode(400)
                .body("errors", hasSize(1))
                .body("errors[0]", is("Field [user_external_id] must be a valid user ID"));
    }

    @Test
    public void shouldReturn_400_whenUserExternalIdIsEmpty() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("user_external_id", ""));
        givenSetup()
                .when()
                .accept(JSON)
                .body(payload)
                .post(format("/v1/api/services/%s/govuk-pay-agreement", service.getExternalId()))
                .then()
                .statusCode(400)
                .body("errors", hasSize(1))
                .body("errors[0]", is("Field [user_external_id] is required"));
    }

    @Test
    public void shouldReturn_400_whenUserExternalIdIsMissing() {
        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("user_id", user.getExternalId()));
        givenSetup()
                .when()
                .accept(JSON)
                .body(payload)
                .post(format("/v1/api/services/%s/govuk-pay-agreement", service.getExternalId()))
                .then()
                .statusCode(400)
                .body("errors", hasSize(1))
                .body("errors[0]", is("Field [user_external_id] is required"));
    }
    
    @Test
    public void shouldReturn_409_whenAgreementAlreadyExists() {
        ServiceEntity serviceEntity = ServiceEntity.from(service);
        serviceEntity.setId(service.getId());
        GovUkPayAgreementDbFixture.govUkPayAgreementDbFixture(databaseHelper)
                .withServiceEntity(serviceEntity)
                .insert();

        JsonNode payload = new ObjectMapper().valueToTree(ImmutableMap.of("user_external_id", user.getExternalId()));
        givenSetup()
                .when()
                .accept(JSON)
                .body(payload)
                .post(format("/v1/api/services/%s/govuk-pay-agreement", service.getExternalId()))
                .then()
                .statusCode(409)
                .body("errors", hasSize(1))
                .body("errors[0]", is("GOV.UK Pay agreement information is already stored for this service"));
    }
    
    @Test
    public void shouldReturnAgreement_whenExists() {
        ServiceEntity serviceEntity = ServiceEntity.from(service);
        serviceEntity.setId(service.getId());
        ZonedDateTime agreementTime = ZonedDateTime.now(ZoneOffset.UTC);
        GovUkPayAgreementDbFixture.govUkPayAgreementDbFixture(databaseHelper)
                .withServiceEntity(serviceEntity)
                .withEmail(user.getEmail())
                .withAgreementTime(agreementTime)
                .insert();

        givenSetup()
                .when()
                .accept(JSON)
                .get(format("/v1/api/services/%s/govuk-pay-agreement", service.getExternalId()))
                .then()
                .statusCode(200)
                .body("email", is(user.getEmail()))
                .body("agreement_time", is(DateTimeUtils.toUTCDateString(agreementTime)));
    }
    
    @Test
    public void shouldReturn404_whenAgreementNotExists() {
        givenSetup()
                .when()
                .accept(JSON)
                .get(format("/v1/api/services/%s/govuk-pay-agreement", service.getExternalId()))
                .then()
                .statusCode(404);
    }
}
