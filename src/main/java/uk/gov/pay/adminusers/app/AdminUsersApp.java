package uk.gov.pay.adminusers.app;

import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;
import com.codahale.metrics.graphite.GraphiteUDP;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.AdminUsersModule;
import uk.gov.pay.adminusers.app.config.PersistenceServiceInitialiser;
import uk.gov.pay.adminusers.app.healthchecks.DependentResourceWaitCommand;
import uk.gov.pay.adminusers.app.healthchecks.MigrateToInitialDbState;
import uk.gov.pay.adminusers.exception.ConflictExceptionMapper;
import uk.gov.pay.adminusers.exception.NotFoundExceptionMapper;
import uk.gov.pay.adminusers.exception.ValidationExceptionMapper;
import uk.gov.pay.adminusers.filters.LoggingMDCRequestFilter;
import uk.gov.pay.adminusers.filters.LoggingMDCResponseFilter;
import uk.gov.pay.adminusers.queue.managed.EventSubscriberQueueMessageReceiver;
import uk.gov.pay.adminusers.resources.EmailResource;
import uk.gov.pay.adminusers.resources.ForgottenPasswordResource;
import uk.gov.pay.adminusers.resources.HealthCheckResource;
import uk.gov.pay.adminusers.resources.InvalidEmailRequestExceptionMapper;
import uk.gov.pay.adminusers.resources.InvalidMerchantDetailsExceptionMapper;
import uk.gov.pay.adminusers.resources.InviteResource;
import uk.gov.pay.adminusers.resources.ResetPasswordResource;
import uk.gov.pay.adminusers.resources.ServiceResource;
import uk.gov.pay.adminusers.resources.ToolboxEndpointResource;
import uk.gov.pay.adminusers.resources.UserResource;
import uk.gov.service.payments.commons.utils.healthchecks.DatabaseHealthCheck;
import uk.gov.service.payments.commons.utils.metrics.DatabaseMetricsService;
import uk.gov.service.payments.commons.utils.prometheus.PrometheusDefaultLabelSampleBuilder;
import uk.gov.service.payments.logging.GovUkPayDropwizardRequestJsonLogLayoutFactory;
import uk.gov.service.payments.logging.LoggingFilter;
import uk.gov.service.payments.logging.LogstashConsoleAppenderFactory;
import uk.gov.service.payments.logging.SentryAppenderFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static java.util.EnumSet.of;
import static javax.servlet.DispatcherType.REQUEST;

public class AdminUsersApp extends Application<AdminUsersConfig> {

    private static final Logger logger = LoggerFactory.getLogger(AdminUsersApp.class);
    
    private static final boolean NON_STRICT_VARIABLE_SUBSTITUTOR = false;
    private static final String SERVICE_METRICS_NODE = "adminusers";
    private static final int GRAPHITE_SENDING_PERIOD_SECONDS = 10;

    @Override
    public void initialize(Bootstrap<AdminUsersConfig> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(NON_STRICT_VARIABLE_SUBSTITUTOR)
                )
        );

        bootstrap.addBundle(new MigrationsBundle<>() {
            @Override
            public DataSourceFactory getDataSourceFactory(AdminUsersConfig configuration) {
                return configuration.getDataSourceFactory();
            }
        });

        bootstrap.addCommand(new DependentResourceWaitCommand());
        bootstrap.addCommand(new MigrateToInitialDbState());
        bootstrap.getObjectMapper().getSubtypeResolver().registerSubtypes(LogstashConsoleAppenderFactory.class);
        bootstrap.getObjectMapper().getSubtypeResolver().registerSubtypes(SentryAppenderFactory.class);
        bootstrap.getObjectMapper().getSubtypeResolver().registerSubtypes(GovUkPayDropwizardRequestJsonLogLayoutFactory.class);
    }

    @Override
    public void run(AdminUsersConfig configuration, Environment environment) {
        final Injector injector = Guice.createInjector(new AdminUsersModule(configuration, environment));
        injector.getInstance(PersistenceServiceInitialiser.class);

        initialiseMetrics(configuration, environment);

        environment.jersey().register(injector.getInstance(LoggingMDCRequestFilter.class));
        environment.jersey().register(injector.getInstance(LoggingMDCResponseFilter.class));
        environment.servlets().addFilter("LoggingFilter", new LoggingFilter())
                .addMappingForUrlPatterns(of(REQUEST), true, "/v1/*");

        environment.healthChecks().register("database", new DatabaseHealthCheck(configuration.getDataSourceFactory()));
        environment.jersey().register(injector.getInstance(UserResource.class));
        environment.jersey().register(injector.getInstance(ServiceResource.class));
        environment.jersey().register(injector.getInstance(ToolboxEndpointResource.class));
        environment.jersey().register(injector.getInstance(ForgottenPasswordResource.class));
        environment.jersey().register(injector.getInstance(InviteResource.class));
        environment.jersey().register(injector.getInstance(ResetPasswordResource.class));
        environment.jersey().register(injector.getInstance(HealthCheckResource.class));
        environment.jersey().register(injector.getInstance(EmailResource.class));

        // Register the custom ExceptionMapper(s)
        environment.jersey().register(new ValidationExceptionMapper());
        environment.jersey().register(new NotFoundExceptionMapper());
        environment.jersey().register(new InvalidEmailRequestExceptionMapper());
        environment.jersey().register(new InvalidMerchantDetailsExceptionMapper());
        environment.jersey().register(new ConflictExceptionMapper());

        environment.lifecycle().manage(injector.getInstance(EventSubscriberQueueMessageReceiver.class));
    }

    private void initialiseMetrics(AdminUsersConfig configuration, Environment environment) {
        DatabaseMetricsService metricsService = new DatabaseMetricsService(configuration.getDataSourceFactory(), environment.metrics(), "adminusers");

        environment
                .lifecycle()
                .scheduledExecutorService("metricscollector")
                .threads(1)
                .build()
                .scheduleAtFixedRate(metricsService::updateMetricData, 0, GRAPHITE_SENDING_PERIOD_SECONDS / 2, TimeUnit.SECONDS);

        initialiseGraphiteMetrics(configuration, environment);
        configuration.getEcsContainerMetadataUriV4().ifPresent(uri -> initialisePrometheusMetrics(environment, uri));
    }

    private void initialisePrometheusMetrics(Environment environment, URI ecsContainerMetadataUri) {
        logger.info("Initialising prometheus metrics.");
        CollectorRegistry collectorRegistry = new CollectorRegistry();
        collectorRegistry.register(new DropwizardExports(environment.metrics(), new PrometheusDefaultLabelSampleBuilder(ecsContainerMetadataUri)));
        environment.admin().addServlet("prometheusMetrics", new MetricsServlet(collectorRegistry)).addMapping("/metrics");
    }

    /**
     * Graphtie metric config to be deleted when we've completely moved to Prometheus
     */
    private static void initialiseGraphiteMetrics(AdminUsersConfig configuration, Environment environment) {
        GraphiteSender graphiteUDP = new GraphiteUDP(configuration.getGraphiteHost(), configuration.getGraphitePort());
        GraphiteReporter.forRegistry(environment.metrics())
                .prefixedWith(SERVICE_METRICS_NODE)
                .build(graphiteUDP)
                .start(GRAPHITE_SENDING_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        new AdminUsersApp().run(args);
    }
}
