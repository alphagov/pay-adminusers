package uk.gov.pay.adminusers.model;

public enum Feature {
    GOVUK_PSP_IS_ADYEN("govuk_psp_is_adyen");

    private final String value;
    
    Feature(String feature) {
        this.value = feature;
    }

    public String getValue() {
        return value;
    }
}
