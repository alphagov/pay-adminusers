package uk.gov.pay.adminusers.app.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Optional;

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
    private final LinksConfig links = new LinksConfig();

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

    @Valid
    @NotNull
    private SqsConfig sqsConfig;

    @NotNull
    private EventSubscriberQueueConfig eventSubscriberQueueConfig;

    @NotNull
    @JsonProperty("ledgerBaseURL")
    private String ledgerBaseUrl;

    @Valid
    @NotNull
    private RestClientConfig restClientConfig;

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

    @JsonProperty("ecsContainerMetadataUriV4")
    private URI ecsContainerMetadataUriV4;

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

    @JsonProperty("sqs")
    public SqsConfig getSqsConfig() {
        return sqsConfig;
    }

    @JsonProperty("eventSubscriberQueue")
    public EventSubscriberQueueConfig getEventSubscriberQueueConfig() {
        return eventSubscriberQueueConfig;
    }

    public String getLedgerBaseUrl() {
        return ledgerBaseUrl;
    }

    public RestClientConfig getRestClientConfig() {
        return restClientConfig;
    }

    public Optional<URI> getEcsContainerMetadataUriV4() {
        return Optional.ofNullable(ecsContainerMetadataUriV4);
    }
}
