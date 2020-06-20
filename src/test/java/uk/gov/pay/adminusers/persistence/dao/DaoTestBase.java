package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.persist.jpa.JpaPersistModule;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jdbi.v3.core.Jdbi;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.infra.GuicedTestEnvironment;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;
import uk.gov.pay.commons.testing.db.PostgresDockerRule;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DaoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(DaoTestBase.class);

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

        databaseHelper = new DatabaseTestHelper(Jdbi.create(postgres.getConnectionUrl(), postgres.getUsername(), postgres.getPassword()));

        try (Connection connection = getConnection()) {
            getLiquibase(connection).update("");
        }

        env = GuicedTestEnvironment.from(jpaModule).start();
    }

    @AfterClass
    public static void cleanUp() {
        try (Connection connection = getConnection()) {
            getLiquibase(connection).dropAll();
        } catch (Exception e) {
            logger.error("Error stopping docker", e);
        }
        env.stop();
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(postgres.getConnectionUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private static Liquibase getLiquibase(Connection conn) throws LiquibaseException {
        return new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(conn));
    }
}
