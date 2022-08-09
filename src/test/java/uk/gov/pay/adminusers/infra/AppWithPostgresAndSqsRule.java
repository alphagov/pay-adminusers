package uk.gov.pay.adminusers.infra;

import com.amazonaws.services.sqs.AmazonSQS;
import com.google.inject.persist.jpa.JpaPersistModule;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.commons.lang3.ArrayUtils;
import org.jdbi.v3.core.Jdbi;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.AdminUsersApp;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;
import uk.gov.service.payments.commons.testing.db.PostgresDockerRule;
import uk.gov.service.payments.commons.testing.port.PortFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static com.google.common.collect.Lists.newArrayList;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

public class AppWithPostgresAndSqsRule implements TestRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppWithPostgresAndSqsRule.class);
    private static final String JPA_UNIT = "AdminUsersUnit";

    private final String configFilePath;
    private final PostgresDockerRule postgres;
    private AmazonSQS sqsClient;
    private final DropwizardAppRule<AdminUsersConfig> app;
    private final RuleChain rules;

    private DatabaseTestHelper databaseTestHelper;
    private int wireMockPort = PortFactory.findFreePort();

    public AppWithPostgresAndSqsRule(ConfigOverride... configOverrides) {
        this("config/test-it-config.yaml", configOverrides);
    }

    public AppWithPostgresAndSqsRule(String configPath, ConfigOverride... configOverrides) {
        configFilePath = resourceFilePath(configPath);
        postgres = new PostgresDockerRule();

        sqsClient = SqsTestDocker.initialise(Collections.singletonList("event-queue"));
        
        ConfigOverride[] newConfigOverrides = List.of(
                config("database.url", postgres.getConnectionUrl()),
                config("database.user", postgres.getUsername()),
                config("database.password", postgres.getPassword()))
                .toArray(new ConfigOverride[0]);
        newConfigOverrides = overrideSqsConfig(newConfigOverrides);
        newConfigOverrides = overrideUrlsConfig(newConfigOverrides);

        app = new DropwizardAppRule<>(
                AdminUsersApp.class,
                configFilePath,
                ArrayUtils.addAll(newConfigOverrides, configOverrides)
        );
        createJpaModule(postgres);
        rules = RuleChain.outerRule(postgres).around(app);
        registerShutdownHook();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return rules.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                LOGGER.info("Clearing database.");
                app.getApplication().run("db", "drop-all", "--confirm-delete-everything", configFilePath);
                app.getApplication().run("migrateToInitialDbState", configFilePath);
                doSecondaryDatabaseMigration();
                restoreDropwizardsLogging();

                DataSourceFactory dataSourceFactory = app.getConfiguration().getDataSourceFactory();
                databaseTestHelper = new DatabaseTestHelper(Jdbi.create(dataSourceFactory.getUrl(), dataSourceFactory.getUser(), dataSourceFactory.getPassword()));

                base.evaluate();
            }
        }, description);
    }

    private void doSecondaryDatabaseMigration() throws SQLException, LiquibaseException {
        try (Connection connection = DriverManager.getConnection(postgres.getConnectionUrl(), postgres.getUsername(), postgres.getPassword())) {
            Liquibase migrator = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            migrator.update("");
        }
    }

    public int getLocalPort() {
        return app.getLocalPort();
    }

    public DatabaseTestHelper getDatabaseTestHelper() {
        return databaseTestHelper;
    }

    public AmazonSQS getSqsClient() {
        return sqsClient;
    }

    public int getWireMockPort() {
        return wireMockPort;
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(postgres::stop));
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

    private ConfigOverride[] overrideSqsConfig(ConfigOverride[] configOverrides) {
        List<ConfigOverride> newConfigOverride = newArrayList(configOverrides);
        newConfigOverride.add(config("sqs.eventSubscriberQueueUrl", SqsTestDocker.getQueueUrl("event-queue")));
        newConfigOverride.add(config("sqs.endpoint", SqsTestDocker.getEndpoint()));
        return newConfigOverride.toArray(new ConfigOverride[0]);
    }
    
    private ConfigOverride[] overrideUrlsConfig(ConfigOverride[] configOverrides) {
        List<ConfigOverride> newConfigOverride = newArrayList(configOverrides);
        newConfigOverride.add(config("notify.notificationBaseURL","http://localhost:" + wireMockPort));
        newConfigOverride.add(config("ledgerBaseURL", "http://localhost:" + wireMockPort));
        return newConfigOverride.toArray(new ConfigOverride[0]);
    }
}
