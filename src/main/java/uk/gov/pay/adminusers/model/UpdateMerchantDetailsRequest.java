package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Optional;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class UpdateMerchantDetailsRequest {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_ADDRESS_LINE1 = "address_line1";
    public static final String FIELD_ADDRESS_LINE2 = "address_line2";
    public static final String FIELD_ADDRESS_CITY = "address_city";
    public static final String FIELD_ADDRESS_POSTCODE = "address_postcode";
    public static final String FIELD_ADDRESS_COUNTRY = "address_country";

    private String name;
    private String addressLine1;
    private String addressLine2;
    private String addressCity;
    private String addressPostcode;
    private String addressCountry;

    public static UpdateMerchantDetailsRequest from(JsonNode node) {
        String name = node.get(FIELD_NAME).asText();
        String addressLine1 = node.get(FIELD_ADDRESS_LINE1).asText();
        String addressCity = node.get(FIELD_ADDRESS_CITY).asText();
        String addressPostcode = node.get(FIELD_ADDRESS_POSTCODE).asText();
        String addressCountry = node.get(FIELD_ADDRESS_COUNTRY).asText();
        String addressLine2 = Optional.ofNullable(node.get(FIELD_ADDRESS_LINE2))
                .map(JsonNode::asText)
                .orElse(null);

        return new UpdateMerchantDetailsRequest(name, addressLine1, addressLine2, addressCity, addressPostcode, addressCountry);
    }

    public UpdateMerchantDetailsRequest(@JsonProperty("name") String name,
                                        @JsonProperty("address_line1") String addressLine1,
                                        @JsonProperty("address_line2") String addressLine2,
                                        @JsonProperty("address_city") String addressCity,
                                        @JsonProperty("address_postcode") String addressPostcode,
                                        @JsonProperty("address_country") String addressCountry) {
        this.name = name;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressCity = addressCity;
        this.addressPostcode = addressPostcode;
        this.addressCountry = addressCountry;
    }

    public String getName() {
        return name;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public String getAddressPostcode() {
        return addressPostcode;
    }

    public String getAddressCountry() {
        return addressCountry;
    }
}
