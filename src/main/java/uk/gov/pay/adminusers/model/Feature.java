package uk.gov.pay.adminusers.model;

public enum Feature {
    TEST_FEATURE("test_feature"),
    TEST_FEATURE_2("test_feature_2");

    private final String value;
    
    Feature(String feature) {
        this.value = feature;
    }

    public String getValue() {
        return value;
    }
}
