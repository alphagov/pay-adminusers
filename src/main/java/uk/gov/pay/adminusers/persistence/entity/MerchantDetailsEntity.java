package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.MerchantDetails;

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

}
