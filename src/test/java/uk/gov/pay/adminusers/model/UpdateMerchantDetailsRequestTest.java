package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

class UpdateMerchantDetailsRequestTest {

    private static ObjectMapper objectMapper = new ObjectMapper();

    private final String name = "name";
    private final String telephoneNumber = "03069990000";
    private final String addressLine1 = "address line1";
    private final String addressLine2 = "address line2";
    private final String addressCity = "city";
    private final String addressCountry = "country";
    private final String addressPostcode = "postcode";
    private final String email = "dd-merchant@example.com";

    @Test
    void shouldConstructMerchantDetails_fromMinimalValidJson() {
        Map<String, Object> payload = Map.of(
                "name", name,
                "address_line1", addressLine1,
                "address_city", addressCity,
                "address_country", addressCountry,
                "address_postcode", addressPostcode);
        JsonNode jsonNode = objectMapper.valueToTree(payload);
        UpdateMerchantDetailsRequest updateMerchantDetailsRequest = UpdateMerchantDetailsRequest.from(jsonNode);
        assertThat(updateMerchantDetailsRequest.getName(), is(name));
        assertThat(updateMerchantDetailsRequest.getAddressLine1(), is(addressLine1));
        assertThat(updateMerchantDetailsRequest.getAddressLine2(), is(nullValue()));
        assertThat(updateMerchantDetailsRequest.getAddressCity(), is(addressCity));
        assertThat(updateMerchantDetailsRequest.getAddressCountry(), is(addressCountry));
        assertThat(updateMerchantDetailsRequest.getAddressPostcode(), is(addressPostcode));
    }

    @Test
    void shouldConstructMerchantDetails_fromCompleteValidJson() {
        Map<String, Object> payload = Map.of(
                "name", name,
                "telephone_number", telephoneNumber,
                "address_line1", addressLine1,
                "address_line2", addressLine2,
                "address_city", addressCity,
                "address_country", addressCountry,
                "address_postcode", addressPostcode,
                "email", email);
        JsonNode jsonNode = objectMapper.valueToTree(payload);
        UpdateMerchantDetailsRequest updateMerchantDetailsRequest = UpdateMerchantDetailsRequest.from(jsonNode);
        assertThat(updateMerchantDetailsRequest.getName(), is(name));
        assertThat(updateMerchantDetailsRequest.getTelephoneNumber(), is(telephoneNumber));
        assertThat(updateMerchantDetailsRequest.getAddressLine1(), is(addressLine1));
        assertThat(updateMerchantDetailsRequest.getAddressLine2(), is(addressLine2));
        assertThat(updateMerchantDetailsRequest.getAddressCity(), is(addressCity));
        assertThat(updateMerchantDetailsRequest.getAddressCountry(), is(addressCountry));
        assertThat(updateMerchantDetailsRequest.getAddressPostcode(), is(addressPostcode));
        assertThat(updateMerchantDetailsRequest.getEmail(), is(email));
    }

}
