package uk.gov.pay.adminusers.infra;

import com.google.inject.persist.jpa.JpaPersistModule;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.commons.lang3.ArrayUtils;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.AdminUsersApp;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;
import uk.gov.service.payments.commons.testing.db.PostgresDockerExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

public class DropwizardAppWithPostgresExtension implements BeforeAllCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(DropwizardAppWithPostgresExtension.class);
    private static final String JPA_UNIT = "AdminUsersUnit";

    private final String configFilePath;
    private final PostgresDockerExtension postgres;
    private final DropwizardAppExtension<AdminUsersConfig> app;

    private DatabaseTestHelper databaseTestHelper;

    public DropwizardAppWithPostgresExtension(ConfigOverride... configOverrides) {
        this("config/test-it-config.yaml", configOverrides);
    }

    public DropwizardAppWithPostgresExtension(String configPath, ConfigOverride... configOverrides) {
        configFilePath = resourceFilePath(configPath);
        postgres = new PostgresDockerExtension();

        ConfigOverride[] postgresOverrides = List.of(
                config("database.url", postgres.getConnectionUrl()),
                config("database.user", postgres.getUsername()),
                config("database.password", postgres.getPassword()))
                .toArray(new ConfigOverride[0]);

        app = new DropwizardAppExtension<>(
                AdminUsersApp.class,
                configFilePath,
                ArrayUtils.addAll(postgresOverrides, configOverrides)
        );

        createJpaModule(postgres);
        registerShutdownHook();

        try {
            // starts dropwizard application. This is required as we don't use DropwizardExtensionsSupport (which starts application)
            // due to config overrides we need at runtime for database, sqs and any custom configuration needed for tests
            app.before();
        } catch (Exception e) {
            LOGGER.error("Exception starting application - {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        LOGGER.info("Clearing database.");
        app.getApplication().run("db", "drop-all", "--confirm-delete-everything", configFilePath);
        app.getApplication().run("migrateToInitialDbState", configFilePath);
        doSecondaryDatabaseMigration();
        restoreDropwizardsLogging();

        DataSourceFactory dataSourceFactory = app.getConfiguration().getDataSourceFactory();
        databaseTestHelper = new DatabaseTestHelper(Jdbi.create(dataSourceFactory.getUrl(),
                dataSourceFactory.getUser(), dataSourceFactory.getPassword()));
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

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(postgres::stop));
    }

    private JpaPersistModule createJpaModule(final PostgresDockerExtension postgres) {
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

}
