package uk.gov.pay.adminusers.app.config;

import io.dropwizard.Configuration;

public class LinksConfig extends Configuration {

    private String selfserviceUrl;

    public String getSelfserviceUrl() {
        return selfserviceUrl;
    }
}
