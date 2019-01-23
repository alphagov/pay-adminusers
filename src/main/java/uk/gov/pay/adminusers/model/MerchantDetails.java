package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MerchantDetails {

    private String name;

    private String telephoneNumber;

    private String addressLine1;

    private String addressLine2;

    private String addressCity;

    private String addressPostcode;

    private String addressCountry;
    
    private String email;

    public MerchantDetails() {
    }

    public MerchantDetails(@JsonProperty("name") String name,
                           @JsonProperty("telephone_number") String telephoneNumber,
                           @JsonProperty("address_line1") String addressLine1,
                           @JsonProperty("address_line2") String addressLine2,
                           @JsonProperty("address_city") String addressCity,
                           @JsonProperty("address_postcode") String addressPostcode,
                           @JsonProperty("address_country") String addressCountry,
                           @JsonProperty("email") String email) {
        this.name = name;
        this.telephoneNumber = telephoneNumber;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressCity = addressCity;
        this.addressPostcode = addressPostcode;
        this.addressCountry = addressCountry;
        this.email = email;
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
    
    public String getEmail() { return this.email; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MerchantDetails that = (MerchantDetails) o;

        return Objects.equals(name, that.name)
                && Objects.equals(telephoneNumber, that.telephoneNumber)
                && Objects.equals(addressLine1, that.addressLine1)
                && Objects.equals(addressLine2, that.addressLine2)
                && Objects.equals(addressCity, that.addressCity)
                && Objects.equals(addressPostcode, that.addressPostcode)
                && Objects.equals(email, that.email)
                && Objects.equals(addressCountry, that.addressCountry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(telephoneNumber, addressLine1, addressLine2, addressCity, addressPostcode, addressCountry, email);
    }
}
