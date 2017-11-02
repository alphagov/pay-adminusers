package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.MerchantDetails;
import uk.gov.pay.adminusers.model.UpdateMerchantDetailsRequest;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class MerchantDetailsEntity {

    @Column(name = "merchant_name")
    private String name;

    @Column(name = "merchant_address_line1")
    private String addressLine1;

    @Column(name = "merchant_address_line2")
    private String addressLine2;

    @Column(name = "merchant_address_city")
    private String addressCity;

    @Column(name = "merchant_address_postcode")
    private String addressPostcode;

    @Column(name = "merchant_address_country")
    private String addressCountry;

    // JPA requires default constructor
    public MerchantDetailsEntity() {
    }

    public MerchantDetailsEntity(String name,
                                 String addressLine1,
                                 String addressLine2,
                                 String addressCity,
                                 String addressPostcode,
                                 String addressCountry) {
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

    public void setName(String name) {
        this.name = name;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressPostcode() {
        return addressPostcode;
    }

    public void setAddressPostcode(String addressPostcode) {
        this.addressPostcode = addressPostcode;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    public MerchantDetails toMerchantDetails() {
        return new MerchantDetails(
                this.name,
                this.addressLine1,
                this.addressLine2,
                this.addressCity,
                this.addressPostcode,
                this.addressCountry
        );
    }

    public static MerchantDetailsEntity from(UpdateMerchantDetailsRequest updateMerchantDetailsRequest) {
        return new MerchantDetailsEntity(
                updateMerchantDetailsRequest.getName(),
                updateMerchantDetailsRequest.getAddressLine1(),
                updateMerchantDetailsRequest.getAddressLine2(),
                updateMerchantDetailsRequest.getAddressCity(),
                updateMerchantDetailsRequest.getAddressPostcode(),
                updateMerchantDetailsRequest.getAddressCountry());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MerchantDetailsEntity that = (MerchantDetailsEntity) o;

        if (!name.equals(that.name)) return false;
        if (!addressLine1.equals(that.addressLine1)) return false;
        if (addressLine2 != null ? !addressLine2.equals(that.addressLine2) : that.addressLine2 != null) return false;
        if (!addressCity.equals(that.addressCity)) return false;
        if (!addressPostcode.equals(that.addressPostcode)) return false;
        return addressCountry.equals(that.addressCountry);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + addressLine1.hashCode();
        result = 31 * result + (addressLine2 != null ? addressLine2.hashCode() : 0);
        result = 31 * result + addressCity.hashCode();
        result = 31 * result + addressPostcode.hashCode();
        result = 31 * result + addressCountry.hashCode();
        return result;
    }
}
