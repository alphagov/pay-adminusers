package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.fixtures.ServiceDbFixture;
import uk.gov.pay.adminusers.model.MerchantDetails;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;

public class EmailResourceIT extends IntegrationTest {

    private ObjectMapper objectMapper = new ObjectMapper();
    private static final String GATEWAY_ACCOUNT_ID = "DIRECT_DEBIT:mdshfsehdtfsdtjg";
    private Map<String, Object> validEmailRequest = Map.of(
            "address", "cake@directdebitteam.test",
            "gateway_account_external_id", GATEWAY_ACCOUNT_ID,
            "template", "MANDATE_CANCELLED",
            "personalisation", Map.of(
                    "mandate reference", "mandatereference",
                    "org name", "cake service"
            )
    );

    @Test
    public void shouldReceiveAPayloadAndSendEmail() {
        ServiceDbFixture.serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(GATEWAY_ACCOUNT_ID)
                .withMerchantDetails(new MerchantDetails(
                        "name", "number", "line1", null, "city",
                        "postcode", "country", "dd-merchant@example.com"
                ))
                .insertService();
        String body = objectMapper.valueToTree(validEmailRequest).toString();
        givenSetup()
                .when()
                .accept(JSON)
                .body(body)
                .post("/v1/emails/send")
                .then()
                .statusCode(200);
    }

}
