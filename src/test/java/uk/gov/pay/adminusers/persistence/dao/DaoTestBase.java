package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.persist.jpa.JpaPersistModule;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.infra.GuicedTestEnvironment;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;
import uk.gov.pay.commons.testing.db.PostgresDockerRule;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.Permission.permission;

public class DaoTestBase {

    private static Logger logger = LoggerFactory.getLogger(DaoTestBase.class);

    @ClassRule
    public static PostgresDockerRule postgres = new PostgresDockerRule();

    protected static DatabaseTestHelper databaseHelper;
    protected static GuicedTestEnvironment env;

    @BeforeClass
    public static void setup() throws Exception {
        final Properties properties = new Properties();
        properties.put("javax.persistence.jdbc.driver", postgres.getDriverClass());
        properties.put("javax.persistence.jdbc.url", postgres.getConnectionUrl());
        properties.put("javax.persistence.jdbc.user", postgres.getUsername());
        properties.put("javax.persistence.jdbc.password", postgres.getPassword());

        JpaPersistModule jpaModule = new JpaPersistModule("AdminUsersUnit").properties(properties);

        databaseHelper = new DatabaseTestHelper(new DBI(postgres.getConnectionUrl(), postgres.getUsername(), postgres.getPassword()));

        try (Connection connection = DriverManager.getConnection(postgres.getConnectionUrl(), postgres.getUsername(), postgres.getPassword())) {

            Liquibase migrator = new Liquibase("config/initial-db-state.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            Liquibase migrator2 = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            migrator.update("");
            migrator2.update("");
        }

        env = GuicedTestEnvironment.from(jpaModule).start();
    }

    @AfterClass
    public static void cleanUp() {
        try (Connection connection = DriverManager.getConnection(
                postgres.getConnectionUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {
            Liquibase migrator = new Liquibase("config/initial-db-state.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            Liquibase migrator2 = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            migrator2.dropAll();
            migrator.dropAll();
        } catch (Exception e) {
            logger.error("Error stopping docker", e);
        }
        env.stop();
    }

    protected Permission aPermission() {
        return permission(randomInt(), "permission-name-" + randomUuid(), "permission-description" + randomUuid());
    }
}
