package uk.gov.pay.adminusers.app.healthchecks;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MigrateToInitialDbState extends ConfiguredCommand<AdminUsersConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateToInitialDbState.class);

    public MigrateToInitialDbState() {
        super("migrateToInitialDbState", "Migrate to initial (selfservice) database state, if necessary");
    }

    @Override
    protected void run(Bootstrap<AdminUsersConfig> bootstrap, Namespace namespace, AdminUsersConfig configuration) {
        try (Connection connection = DriverManager.getConnection(
                configuration.getDataSourceFactory().getUrl(),
                configuration.getDataSourceFactory().getUser(),
                configuration.getDataSourceFactory().getPassword())) {
            Liquibase migrator = new Liquibase("config/initial-db-state.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            migrator.update("");
        } catch (LiquibaseException|SQLException e) {
            LOGGER.error("Error performing liquibase initial database migration", e);
            throw new RuntimeException(e);
        }
    }
}
