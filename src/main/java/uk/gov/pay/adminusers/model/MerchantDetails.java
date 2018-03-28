package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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

    public MerchantDetails() {
    }

    public MerchantDetails(@JsonProperty("name") String name,
                           @JsonProperty("telephone_number") String telephoneNumber,
                           @JsonProperty("address_line1") String addressLine1,
                           @JsonProperty("address_line2") String addressLine2,
                           @JsonProperty("address_city") String addressCity,
                           @JsonProperty("address_postcode") String addressPostcode,
                           @JsonProperty("address_country") String addressCountry) {
        this.name = name;
        this.telephoneNumber = telephoneNumber;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressCity = addressCity;
        this.addressPostcode = addressPostcode;
        this.addressCountry = addressCountry;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MerchantDetails that = (MerchantDetails) o;

        if (!name.equals(that.name)) return false;
        if (telephoneNumber != null ? !telephoneNumber.equals(that.telephoneNumber) : that.telephoneNumber != null)
            return false;
        if (!addressLine1.equals(that.addressLine1)) return false;
        if (addressLine2 != null ? !addressLine2.equals(that.addressLine2) : that.addressLine2 != null) return false;
        if (!addressCity.equals(that.addressCity)) return false;
        if (!addressPostcode.equals(that.addressPostcode)) return false;
        return addressCountry.equals(that.addressCountry);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (telephoneNumber != null ? telephoneNumber.hashCode() : 0);
        result = 31 * result + addressLine1.hashCode();
        result = 31 * result + (addressLine2 != null ? addressLine2.hashCode() : 0);
        result = 31 * result + addressCity.hashCode();
        result = 31 * result + addressPostcode.hashCode();
        result = 31 * result + addressCountry.hashCode();
        return result;
    }

}
