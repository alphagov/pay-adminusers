package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Optional;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
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

    @Schema(example = "organisation name", requiredMode = REQUIRED)
    private String name;
    @Schema(example = "447700900000")
    private String telephoneNumber;
    @Schema(example = "Address line 1", requiredMode = REQUIRED)
    private String addressLine1;
    @Schema(example = "Address line 2")
    private String addressLine2;
    @Schema(example = "London", requiredMode = REQUIRED)
    private String addressCity;
    @Schema(example = "E1 8XX", requiredMode = REQUIRED)
    private String addressPostcode;
    @Schema(example = "GB", requiredMode = REQUIRED)
    private String addressCountry;
    @Schema(example = "email@example.com")
    private String email;
    @Schema(example = "http://www.example.org")
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
