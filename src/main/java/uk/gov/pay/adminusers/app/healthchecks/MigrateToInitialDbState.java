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

import java.sql.*;

public class MigrateToInitialDbState extends ConfiguredCommand<AdminUsersConfig> {

    private static Logger logger = LoggerFactory.getLogger(MigrateToInitialDbState.class);

    public MigrateToInitialDbState() {
        super("migrateToInitialDbState", "Migrate to initial (selfservice) database state, if necessary");
    }

    @Override
    protected void run(Bootstrap<AdminUsersConfig> bootstrap, Namespace namespace, AdminUsersConfig configuration) throws Exception {
        Connection connection = null;
        try {
            connection = getDatabaseConnection(configuration);
            PreparedStatement statement = connection.prepareStatement("select exists (select * from pg_tables where tablename='users')");
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                boolean usersExists = resultSet.getBoolean(1);
                if (!usersExists) {
                    logger.info("Users table not found. Preparing for the initial database migration..");
                    performInitialMigration(connection);
                } else {
                    logger.info("Users table found in current environment. Not required for the initial database migration");
                }
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            logger.error("Error during initial DB setup", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private void performInitialMigration(Connection connection) {
        try {
            Liquibase migrator = new Liquibase("config/initial-db-state.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            migrator.update("");
        } catch (LiquibaseException e) {
            logger.error("Error performing liquibase initial database migration - {}", e);
            throw new RuntimeException(e);
        }
    }

    private Connection getDatabaseConnection(AdminUsersConfig configuration) throws SQLException {
        return DriverManager.getConnection(
                configuration.getDataSourceFactory().getUrl(),
                configuration.getDataSourceFactory().getUser(),
                configuration.getDataSourceFactory().getPassword());
    }
}
