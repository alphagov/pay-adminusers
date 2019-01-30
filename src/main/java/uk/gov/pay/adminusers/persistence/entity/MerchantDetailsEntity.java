package uk.gov.pay.adminusers.persistence.entity;

import uk.gov.pay.adminusers.model.MerchantDetails;
import uk.gov.pay.adminusers.model.UpdateMerchantDetailsRequest;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class MerchantDetailsEntity {

    @Column(name = "merchant_name")
    private String name;

    @Column(name = "merchant_telephone_number")
    private String telephoneNumber;

    @Column(name = "merchant_address_line1")
    private String addressLine1;

    @Column(name = "merchant_address_line2")
    private String addressLine2;

    @Column(name = "merchant_address_city")
    private String addressCity;

    @Column(name = "merchant_address_postcode")
    private String addressPostcode;

    @Column(name = "merchant_address_country")
    private String addressCountryCode;

    @Column(name = "merchant_email")
    private String email;

    // JPA requires default constructor
    public MerchantDetailsEntity() {
    }

    public MerchantDetailsEntity(String name,
                                 String telephoneNumber,
                                 String addressLine1,
                                 String addressLine2,
                                 String addressCity,
                                 String addressPostcode,
                                 String addressCountry,
                                 String email) {
        this.name = name;
        this.telephoneNumber = telephoneNumber;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressCity = addressCity;
        this.addressPostcode = addressPostcode;
        this.addressCountryCode = addressCountry;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
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

    public String getAddressCountryCode() {
        return addressCountryCode;
    }

    public void setAddressCountryCode(String addressCountryCode) {
        this.addressCountryCode = addressCountryCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public MerchantDetails toMerchantDetails() {
        return new MerchantDetails(
                this.name,
                this.telephoneNumber,
                this.addressLine1,
                this.addressLine2,
                this.addressCity,
                this.addressPostcode,
                this.addressCountryCode,
                this.email
        );
    }

    public static MerchantDetailsEntity from(UpdateMerchantDetailsRequest updateMerchantDetailsRequest) {
        return new MerchantDetailsEntity(
                updateMerchantDetailsRequest.getName(),
                updateMerchantDetailsRequest.getTelephoneNumber(),
                updateMerchantDetailsRequest.getAddressLine1(),
                updateMerchantDetailsRequest.getAddressLine2(),
                updateMerchantDetailsRequest.getAddressCity(),
                updateMerchantDetailsRequest.getAddressPostcode(),
                updateMerchantDetailsRequest.getAddressCountry(),
                updateMerchantDetailsRequest.getEmail());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MerchantDetailsEntity that = (MerchantDetailsEntity) o;

        return Objects.equals(name, that.name)
                && Objects.equals(telephoneNumber, that.telephoneNumber)
                && Objects.equals(addressLine1, that.addressLine1)
                && Objects.equals(addressLine2, that.addressLine2)
                && Objects.equals(addressCity, that.addressCity)
                && Objects.equals(addressPostcode, that.addressPostcode)
                && Objects.equals(email, that.email)
                && Objects.equals(addressCountryCode, that.addressCountryCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, telephoneNumber, addressLine1, addressLine2, addressCity,
                addressPostcode, addressCountryCode, email);
    }
}
