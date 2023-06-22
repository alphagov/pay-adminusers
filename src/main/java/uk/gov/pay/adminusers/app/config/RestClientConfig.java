package uk.gov.pay.adminusers.app.config;

import io.dropwizard.core.Configuration;

public class RestClientConfig extends Configuration {

    private String disabledSecureConnection;

    public RestClientConfig() {
    }

    public Boolean isDisabledSecureConnection() {
        return "true".equals(disabledSecureConnection);
    }
}

