package uk.gov.pay.adminusers.model;

public enum Feature {
    TEST_FEATURE("test_feature");

    private final String value;
    
    Feature(String feature) {
        this.value = feature;
    }

    public String getValue() {
        return value;
    }
}
