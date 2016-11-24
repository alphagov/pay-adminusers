package uk.gov.pay.adminusers.app;

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
import uk.gov.pay.adminusers.app.healthchecks.DatabaseHealthCheck;
import uk.gov.pay.adminusers.app.healthchecks.DependentResourceWaitCommand;
import uk.gov.pay.adminusers.app.healthchecks.Ping;
import uk.gov.pay.adminusers.resources.HealthCheckResource;

public class AdminUsersApp extends Application<AdminUsersConfig> {

    private static final boolean NON_STRICT_VARIABLE_SUBSTITUTOR = false;

    @Override
    public void initialize(Bootstrap<AdminUsersConfig> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(NON_STRICT_VARIABLE_SUBSTITUTOR)
                )
        );

        //TODO: disabling till the next pull request, until the AWS DB environments are ready
//        bootstrap.addBundle(new MigrationsBundle<AdminUsersConfig>() {
//            @Override
//            public DataSourceFactory getDataSourceFactory(AdminUsersConfig configuration) {
//                return configuration.getDataSourceFactory();
//            }
//        });
//
//        bootstrap.addCommand(new DependentResourceWaitCommand());
    }

    @Override
    public void run(AdminUsersConfig configuration, Environment environment) throws Exception {
        final Injector injector = Guice.createInjector(new AdminUsersModule(configuration, environment));

        environment.healthChecks().register("ping", new Ping());
        //TODO: disabling till the next pull request, until the AWS DB environments are ready
//        injector.getInstance(PersistenceServiceInitialiser.class);
//        environment.healthChecks().register("database", injector.getInstance(DatabaseHealthCheck.class));

        environment.jersey().register(injector.getInstance(HealthCheckResource.class));

    }

    public static void main(String[] args) throws Exception {
        new AdminUsersApp().run(args);
    }
}
