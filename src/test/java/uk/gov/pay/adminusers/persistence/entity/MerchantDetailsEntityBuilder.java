package uk.gov.pay.adminusers.persistence.entity;

public final class MerchantDetailsEntityBuilder {
    private String name = "test-name";
    private String telephoneNumber = "0123456789";
    private String addressLine1 = "test-line-1";
    private String addressLine2 = "test-line-2";
    private String addressCity = "test-address-2";
    private String addressPostcode = "test-postcode";
    private String addressCountryCode = "GB";
    private String email = "merchant@example.com";

    private MerchantDetailsEntityBuilder() {
    }

    public static MerchantDetailsEntityBuilder aMerchantDetailsEntity() {
        return new MerchantDetailsEntityBuilder();
    }

    public MerchantDetailsEntityBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public MerchantDetailsEntityBuilder withTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
        return this;
    }

    public MerchantDetailsEntityBuilder withAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
        return this;
    }

    public MerchantDetailsEntityBuilder withAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
        return this;
    }

    public MerchantDetailsEntityBuilder withAddressCity(String addressCity) {
        this.addressCity = addressCity;
        return this;
    }

    public MerchantDetailsEntityBuilder withAddressPostcode(String addressPostcode) {
        this.addressPostcode = addressPostcode;
        return this;
    }

    public MerchantDetailsEntityBuilder withAddressCountryCode(String addressCountryCode) {
        this.addressCountryCode = addressCountryCode;
        return this;
    }

    public MerchantDetailsEntityBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public MerchantDetailsEntity build() {
        MerchantDetailsEntity merchantDetailsEntity = new MerchantDetailsEntity();
        merchantDetailsEntity.setName(name);
        merchantDetailsEntity.setTelephoneNumber(telephoneNumber);
        merchantDetailsEntity.setAddressLine1(addressLine1);
        merchantDetailsEntity.setAddressLine2(addressLine2);
        merchantDetailsEntity.setAddressCity(addressCity);
        merchantDetailsEntity.setAddressPostcode(addressPostcode);
        merchantDetailsEntity.setAddressCountryCode(addressCountryCode);
        merchantDetailsEntity.setEmail(email);
        return merchantDetailsEntity;
    }
}
