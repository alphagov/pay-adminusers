package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Optional;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class UpdateMerchantDetailsRequest {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_TELEPHONE_NUMBER = "telephone_number";
    private static final String FIELD_ADDRESS_LINE1 = "address_line1";
    private static final String FIELD_ADDRESS_LINE2 = "address_line2";
    private static final String FIELD_ADDRESS_CITY = "address_city";
    private static final String FIELD_ADDRESS_POSTCODE = "address_postcode";
    private static final String FIELD_ADDRESS_COUNTRY = "address_country";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_URL = "url";

    private String name;
    private String telephoneNumber;
    private String addressLine1;
    private String addressLine2;
    private String addressCity;
    private String addressPostcode;
    private String addressCountry;
    private String email;
    private String url;

    public static UpdateMerchantDetailsRequest from(JsonNode node) {
        String name = node.get(FIELD_NAME).asText();
        String telephoneNumber = Optional.ofNullable(node.get(FIELD_TELEPHONE_NUMBER))
                .map(JsonNode::asText)
                .orElse(null);
        String addressLine1 = node.get(FIELD_ADDRESS_LINE1).asText();
        String addressCity = node.get(FIELD_ADDRESS_CITY).asText();
        String addressPostcode = node.get(FIELD_ADDRESS_POSTCODE).asText();
        String addressCountry = node.get(FIELD_ADDRESS_COUNTRY).asText();
        String addressLine2 = Optional.ofNullable(node.get(FIELD_ADDRESS_LINE2))
                .map(JsonNode::asText)
                .orElse(null);
        String email = Optional.ofNullable(node.get(FIELD_EMAIL)).map(JsonNode::asText).orElse(null);
        String url = Optional.ofNullable(node.get(FIELD_URL)).map(JsonNode::asText).orElse(null);

        return new UpdateMerchantDetailsRequest(
                name, telephoneNumber, addressLine1, addressLine2, addressCity, addressPostcode,
                addressCountry, email, url
        );
    }

    public UpdateMerchantDetailsRequest(@JsonProperty("name") String name,
                                        @JsonProperty("telephone_number") String telephoneNumber,
                                        @JsonProperty("address_line1") String addressLine1,
                                        @JsonProperty("address_line2") String addressLine2,
                                        @JsonProperty("address_city") String addressCity,
                                        @JsonProperty("address_postcode") String addressPostcode,
                                        @JsonProperty("address_country") String addressCountry,
                                        @JsonProperty("email") String email,
                                        @JsonProperty("url") String url) {
        this.name = name;
        this.telephoneNumber = telephoneNumber;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressCity = addressCity;
        this.addressPostcode = addressPostcode;
        this.addressCountry = addressCountry;
        this.email = email;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
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

    public String getEmail() {
        return this.email;
    }

    public String getUrl() {
        return this.url;
    }
}
