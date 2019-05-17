package uk.gov.pay.adminusers.resources;

import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.GovUkPayAgreementDbFixture.govUkPayAgreementDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

public class ServiceResourceSendLiveAccountCreatedEmailIT extends IntegrationTest {
    @Test
    public void shouldSendEmail() {
        Service service = serviceDbFixture(databaseHelper).insertService();
        govUkPayAgreementDbFixture(databaseHelper).withServiceId(service.getId()).insert();

        givenSetup()
                .when()
                .post(format("/v1/api/services/%s/send-live-email", service.getExternalId()))
                .then()
                .statusCode(200);
    }

    @Test
    public void shouldReturn_404_whenServiceNotFound() {
        givenSetup()
                .when()
                .post(format("/v1/api/services/%s/send-live-email", "123"))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldReturn_409_whenAgreementNotSigned() {
        Service service = serviceDbFixture(databaseHelper).insertService();
        givenSetup()
                .when()
                .post(format("/v1/api/services/%s/send-live-email", service.getExternalId()))
                .then()
                .statusCode(409)
                .body("errors", hasSize(1))
                .body("errors[0]", is("Nobody from this service is on record as having agreed to the legal terms"));
    }
}
