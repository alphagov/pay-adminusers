package uk.gov.pay.adminusers.app.config;

import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;

public class ProxyConfiguration extends Configuration{

    @NotNull
    private String host;

    @NotNull
    private Integer port;

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }
}
