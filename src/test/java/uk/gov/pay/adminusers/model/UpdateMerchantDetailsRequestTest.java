package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class UpdateMerchantDetailsRequestTest {

    private final String name = "name";
    private final String telephoneNumber = "03069990000";
    private final String addressLine1 = "address line1";
    private final String addressLine2 = "address line2";
    private final String addressCity = "city";
    private final String addressCountry = "country";
    private final String addressPostcode = "postcode";
    private final String email = "dd-merchant@example.com";

    @Test
    public void shouldConstructMerchantDetails_fromMinimalValidJson() {
        Map<String, Object> payload = ImmutableMap.<String, Object>builder()
                .put("name", name)
                .put("address_line1", addressLine1)
                .put("address_city", addressCity)
                .put("address_country", addressCountry)
                .put("address_postcode", addressPostcode)
                .build();
        JsonNode jsonNode = new ObjectMapper().valueToTree(payload);
        UpdateMerchantDetailsRequest updateMerchantDetailsRequest = UpdateMerchantDetailsRequest.from(jsonNode);
        assertThat(updateMerchantDetailsRequest.getName(), is(name));
        assertThat(updateMerchantDetailsRequest.getAddressLine1(), is(addressLine1));
        assertThat(updateMerchantDetailsRequest.getAddressLine2(), is(nullValue()));
        assertThat(updateMerchantDetailsRequest.getAddressCity(), is(addressCity));
        assertThat(updateMerchantDetailsRequest.getAddressCountry(), is(addressCountry));
        assertThat(updateMerchantDetailsRequest.getAddressPostcode(), is(addressPostcode));
    }

    @Test
    public void shouldConstructMerchantDetails_fromCompleteValidJson() {
        Map<String, Object> payload = ImmutableMap.<String, Object>builder()
                .put("name", name)
                .put("telephone_number", telephoneNumber)
                .put("address_line1", addressLine1)
                .put("address_line2", addressLine2)
                .put("address_city", addressCity)
                .put("address_country", addressCountry)
                .put("address_postcode", addressPostcode)
                .put("email", email)
                .build();
        JsonNode jsonNode = new ObjectMapper().valueToTree(payload);
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
