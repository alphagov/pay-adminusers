package uk.gov.pay.adminusers.app.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class AdminUsersConfig extends Configuration {

    private static final Logger logger = PayLoggerFactory.getLogger(AdminUsersConfig.class);

    @Valid
    @NotNull
    private DataSourceFactory dataSourceFactory;

    @Valid
    @NotNull
    private JPAConfiguration jpaConfiguration;

    @NotNull
    private String graphiteHost;
    @NotNull
    private String graphitePort;

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
    private ProxyConfiguration proxyConfiguration;

    @NotNull
    private Integer forgottenPasswordExpiryMinutes;

    @NotNull
    private SecondFactorAuthConfiguration secondFactorAuthConfiguration;

    @JsonProperty("secondFactorAuthentication")
    public SecondFactorAuthConfiguration getSecondFactorAuthConfiguration() {
        return secondFactorAuthConfiguration;
    }

    @JsonProperty("proxy")
    public ProxyConfiguration getProxyConfiguration() {
        return proxyConfiguration;
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

    public String getGraphitePort() {
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

    public Integer getForgottenPasswordExpiryMinutes() {
        return forgottenPasswordExpiryMinutes;
    }
}
