package uk.gov.pay.adminusers.utils;

import com.google.inject.persist.jpa.JpaPersistModule;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.AdminUsersApp;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;

import java.util.List;
import java.util.Properties;

import static com.google.common.collect.Lists.newArrayList;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

public class DropwizardAppWithPostgresRule implements TestRule {
    private static final Logger logger = LoggerFactory.getLogger(DropwizardAppWithPostgresRule.class);
    public static final String JPA_UNIT = "ConnectorUnit";

    private final String configFilePath;
    private final String initialDatabaseMigrationsPath;
    private final String secondaryDatabaseMigrationsPath;
    private final PostgresDockerRule postgres;
    private final DropwizardAppRule<AdminUsersConfig> app;
    private final RuleChain rules;
    private final JpaPersistModule persistModule;

    private DatabaseTestHelper databaseTestHelper;

    public DropwizardAppWithPostgresRule() {
        this("config/test-it-config.yaml");
    }

    public DropwizardAppWithPostgresRule(ConfigOverride... configOverrides) {
        this("config/test-it-config.yaml", configOverrides);
    }

    public DropwizardAppWithPostgresRule(String configPath, ConfigOverride... configOverrides) {
        configFilePath = resourceFilePath(configPath);
        initialDatabaseMigrationsPath = resourceFilePath("config/initial-testdb-state.xml");
        secondaryDatabaseMigrationsPath = resourceFilePath("migrations.xml");
        postgres = new PostgresDockerRule();
        List<ConfigOverride> cfgOverrideList = newArrayList(configOverrides);
        cfgOverrideList.add(config("database.url", postgres.getConnectionUrl()));
        cfgOverrideList.add(config("database.user", postgres.getUsername()));
        cfgOverrideList.add(config("database.password", postgres.getPassword()));

        app = new DropwizardAppRule<>(
                AdminUsersApp.class,
                configFilePath,
                cfgOverrideList.toArray(new ConfigOverride[cfgOverrideList.size()])
        );
        persistModule = createJpaModule(postgres);
        rules = RuleChain.outerRule(postgres).around(app);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return rules.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                logger.info("Clearing database.");
                app.getApplication().run("db", "drop-all", "--confirm-delete-everything", configFilePath);
                app.getApplication().run("db", "migrate", configFilePath, "--migrations", initialDatabaseMigrationsPath);
                app.getApplication().run("db", "migrate", configFilePath, "--migrations", secondaryDatabaseMigrationsPath);

                restoreDropwizardsLogging();

                DataSourceFactory dataSourceFactory = app.getConfiguration().getDataSourceFactory();
                databaseTestHelper = new DatabaseTestHelper(new DBI(dataSourceFactory.getUrl(), dataSourceFactory.getUser(), dataSourceFactory.getPassword()));

                base.evaluate();
            }
        }, description);
    }

    public AdminUsersConfig getConf() {
        return app.getConfiguration();
    }

    public Environment getEnvironment(){
        return app.getEnvironment();
    }

    public int getLocalPort() {
        return app.getLocalPort();
    }

    public int getAdminPort() {
        return app.getAdminPort();
    }

    public DatabaseTestHelper getDatabaseTestHelper() {
        return databaseTestHelper;
    }

    public void stopPostgres() {
        postgres.stop();
    }

    private JpaPersistModule createJpaModule(final PostgresDockerRule postgres) {
        final Properties properties = new Properties();
        properties.put("javax.persistence.jdbc.driver", postgres.getDriverClass());
        properties.put("javax.persistence.jdbc.url", postgres.getConnectionUrl());
        properties.put("javax.persistence.jdbc.user", postgres.getUsername());
        properties.put("javax.persistence.jdbc.password", postgres.getPassword());

        final JpaPersistModule jpaModule = new JpaPersistModule(JPA_UNIT);
        jpaModule.properties(properties);

        return jpaModule;
    }

    private void restoreDropwizardsLogging() {
        app.getConfiguration().getLoggingFactory().configure(app.getEnvironment().metrics(),
                app.getApplication().getName());
    }

    public JpaPersistModule getPersistModule() {
        return persistModule;
    }
}
