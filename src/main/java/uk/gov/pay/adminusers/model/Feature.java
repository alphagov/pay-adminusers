package uk.gov.pay.adminusers.model;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum Feature {
    GOVUK_PSP_IS_ADYEN("govuk_psp_is_adyen");

    private final String value;
    
    Feature(String feature) {
        this.value = feature;
    }

    public String getValue() {
        return value;
    }
    
    public static String getValidValues() {
        return Arrays.stream(Feature.values())
                .map(Feature::getValue)
                .collect(Collectors.joining(", "));
    }
}
