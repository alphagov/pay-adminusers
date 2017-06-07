package uk.gov.pay.adminusers.app.config;

import io.dropwizard.Configuration;

public class LinksConfig extends Configuration {

    private String selfserviceUrl;
    private String frontendUrl;

    public String getSelfserviceUrl() {
        return selfserviceUrl;
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }
}
