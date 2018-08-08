package uk.gov.pay.adminusers.app;

import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;
import com.codahale.metrics.graphite.GraphiteUDP;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.AdminUsersModule;
import uk.gov.pay.adminusers.app.config.PersistenceServiceInitialiser;
import uk.gov.pay.adminusers.app.filters.LoggingFilter;
import uk.gov.pay.adminusers.app.healthchecks.DatabaseHealthCheck;
import uk.gov.pay.adminusers.app.healthchecks.DependentResourceWaitCommand;
import uk.gov.pay.adminusers.app.healthchecks.MigrateToInitialDbState;
import uk.gov.pay.adminusers.app.healthchecks.Ping;
import uk.gov.pay.adminusers.app.util.TrustingSSLSocketFactory;
import uk.gov.pay.adminusers.exception.ServiceNotFoundExceptionMapper;
import uk.gov.pay.adminusers.exception.ValidationExceptionMapper;
import uk.gov.pay.adminusers.resources.EmailResource;
import uk.gov.pay.adminusers.resources.ForgottenPasswordResource;
import uk.gov.pay.adminusers.resources.HealthCheckResource;
import uk.gov.pay.adminusers.resources.InvalidEmailRequestExceptionMapper;
import uk.gov.pay.adminusers.resources.InvalidMerchantDetailsExceptionMapper;
import uk.gov.pay.adminusers.resources.InviteResource;
import uk.gov.pay.adminusers.resources.ResetPasswordResource;
import uk.gov.pay.adminusers.resources.ServiceResource;
import uk.gov.pay.adminusers.resources.UserResource;
import uk.gov.pay.commons.utils.xray.Xray;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.util.concurrent.TimeUnit;

import static java.util.EnumSet.of;
import static javax.servlet.DispatcherType.REQUEST;
import static uk.gov.pay.adminusers.resources.UserResource.API_VERSION_PATH;

public class AdminUsersApp extends Application<AdminUsersConfig> {

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

        bootstrap.addBundle(new MigrationsBundle<AdminUsersConfig>() {
            @Override
            public DataSourceFactory getDataSourceFactory(AdminUsersConfig configuration) {
                return configuration.getDataSourceFactory();
            }
        });
        
        bootstrap.addCommand(new DependentResourceWaitCommand());
        bootstrap.addCommand(new MigrateToInitialDbState());
    }

    @Override
    public void run(AdminUsersConfig configuration, Environment environment) throws Exception {
        final Injector injector = Guice.createInjector(new AdminUsersModule(configuration, environment));
        injector.getInstance(PersistenceServiceInitialiser.class);

        initialiseMetrics(configuration, environment);

        environment.servlets().addFilter("LoggingFilter", new LoggingFilter())
                .addMappingForUrlPatterns(of(REQUEST), true, API_VERSION_PATH + "/*");

        environment.healthChecks().register("ping", new Ping());
        environment.healthChecks().register("database", injector.getInstance(DatabaseHealthCheck.class));
        environment.jersey().register(injector.getInstance(UserResource.class));
        environment.jersey().register(injector.getInstance(ServiceResource.class));
        environment.jersey().register(injector.getInstance(ForgottenPasswordResource.class));
        environment.jersey().register(injector.getInstance(InviteResource.class));
        environment.jersey().register(injector.getInstance(ResetPasswordResource.class));
        environment.jersey().register(injector.getInstance(HealthCheckResource.class));
        environment.jersey().register(injector.getInstance(EmailResource.class));

        // Register the custom ExceptionMapper(s)
        environment.jersey().register(new ValidationExceptionMapper());
        environment.jersey().register(new ServiceNotFoundExceptionMapper());
        environment.jersey().register(new InvalidEmailRequestExceptionMapper());
        environment.jersey().register(new InvalidMerchantDetailsExceptionMapper());

        setGlobalProxies(configuration);
        
        Xray.init(environment, "pay-adminusers",API_VERSION_PATH + "/*");
    }

    private void initialiseMetrics(AdminUsersConfig configuration, Environment environment) {
        GraphiteSender graphiteUDP = new GraphiteUDP(configuration.getGraphiteHost(), Integer.valueOf(configuration.getGraphitePort()));
        GraphiteReporter.forRegistry(environment.metrics())
                .prefixedWith(SERVICE_METRICS_NODE)
                .build(graphiteUDP)
                .start(GRAPHITE_SENDING_PERIOD_SECONDS, TimeUnit.SECONDS);

    }

    private void setGlobalProxies(AdminUsersConfig configuration) {
        SSLSocketFactory socketFactory = new TrustingSSLSocketFactory();
        HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory);

        System.setProperty("https.proxyHost", configuration.getProxyConfiguration().getHost());
        System.setProperty("https.proxyPort", configuration.getProxyConfiguration().getPort().toString());
    }

    public static void main(String[] args) throws Exception {
        new AdminUsersApp().run(args);
    }
}
