package uk.gov.pay.adminusers.app.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class AdminUsersConfig extends Configuration {

    @Valid
    @NotNull
    private DataSourceFactory dataSourceFactory;

    @Valid
    @NotNull
    private JPAConfiguration jpaConfiguration;

    @NotNull
    private String graphiteHost;
    @NotNull
    private Integer graphitePort;

    @NotNull
    private String baseUrl;

    @Valid
    @NotNull
    private LinksConfig links = new LinksConfig();

    @NotNull
    private Integer loginAttemptCap;

    @NotNull
    private NotifyConfiguration notifyConfiguration;

    @NotNull
    private NotifyDirectDebitConfiguration notifyDirectDebitConfiguration;

    @NotNull
    private Integer forgottenPasswordExpiryMinutes;

    @NotNull
    private SecondFactorAuthConfiguration secondFactorAuthConfiguration;

    @JsonProperty("secondFactorAuthentication")
    public SecondFactorAuthConfiguration getSecondFactorAuthConfiguration() {
        return secondFactorAuthConfiguration;
    }

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return dataSourceFactory;
    }

    @JsonProperty("jpa")
    public JPAConfiguration getJpaConfiguration() {
        return jpaConfiguration;
    }

    public String getGraphiteHost() {
        return graphiteHost;
    }

    public Integer getGraphitePort() {
        return graphitePort;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Integer getLoginAttemptCap() {
        return loginAttemptCap;
    }

    public LinksConfig getLinks() {
        return links;
    }

    @JsonProperty("notify")
    public NotifyConfiguration getNotifyConfiguration() {
        return notifyConfiguration;
    }

    @JsonProperty("notifyDirectDebit")
    public NotifyDirectDebitConfiguration getNotifyDirectDebitConfiguration() {
        return notifyDirectDebitConfiguration;
    }

    public Integer getForgottenPasswordExpiryMinutes() {
        return forgottenPasswordExpiryMinutes;
    }
}
