package uk.gov.pay.adminusers.app.config;

import io.dropwizard.Configuration;

public class LinksConfig extends Configuration {

    private String selfserviceUrl;
    private String selfserviceInvitesUrl;
    private String selfserviceLoginUrl;
    private String selfserviceForgottenPasswordUrl;
    private String supportUrl;
    private String selfserviceServicesUrl;

    public String getSelfserviceUrl() {
        return selfserviceUrl;
    }

    public String getSelfserviceInvitesUrl() {
        return selfserviceInvitesUrl;
    }

    public String getSelfserviceLoginUrl() {
        return selfserviceLoginUrl;
    }

    public String getSelfserviceForgottenPasswordUrl() {
        return selfserviceForgottenPasswordUrl;
    }

    public String getSupportUrl() {
        return supportUrl;
    }
    
    public String getSelfserviceServicesUrl() {
        return selfserviceServicesUrl;
    }
}
