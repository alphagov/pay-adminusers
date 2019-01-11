package uk.gov.pay.adminusers.persistence.dao;

import com.google.inject.persist.jpa.JpaPersistModule;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.After;
import org.junit.Before;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.gov.pay.adminusers.infra.GuicedTestEnvironment;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.Permission.permission;
import static uk.gov.pay.adminusers.model.Role.role;

public class DaoTestBase {

    private static Logger logger = LoggerFactory.getLogger(DaoTestBase.class);

    public PostgreSQLContainer postgreSQLContainer;

    protected static DatabaseTestHelper databaseHelper;
    private static JpaPersistModule jpaModule;
    protected static GuicedTestEnvironment env;

    @Before
    public void setup() throws Exception {
        postgreSQLContainer = new PostgreSQLContainer();
        postgreSQLContainer.start();

        final Properties properties = new Properties();
        properties.put("javax.persistence.jdbc.driver", postgreSQLContainer.getDriverClassName());
        properties.put("javax.persistence.jdbc.url", postgreSQLContainer.getJdbcUrl());
        properties.put("javax.persistence.jdbc.user", postgreSQLContainer.getUsername());
        properties.put("javax.persistence.jdbc.password", postgreSQLContainer.getPassword());

        jpaModule = new JpaPersistModule("AdminUsersUnit");
        jpaModule.properties(properties);

        databaseHelper = new DatabaseTestHelper(new DBI(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword()));

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());

            Liquibase migrator = new Liquibase("config/initial-db-state.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            Liquibase migrator2 = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            migrator.update("");
            migrator2.update("");
        } finally {
            if (connection != null)
                connection.close();
        }

        env = GuicedTestEnvironment.from(jpaModule).start();
    }

    @After
    public void tearDown() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());
            Liquibase migrator = new Liquibase("config/initial-db-state.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            Liquibase migrator2 = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            migrator2.dropAll();
            migrator.dropAll();
            connection.close();
        } catch (Exception e) {
            logger.error("Error stopping docker", e);
        }
        env.stop();
        postgreSQLContainer.stop();
    }

    protected Role aRole() {
        return aRole("role-name-" + randomUuid());
    }

    protected Role aRole(String name) {
        return role(randomInt(), name, "role-description" + randomUuid());
    }

    protected Permission aPermission() {
        return permission(randomInt(), "permission-name-" + randomUuid(), "permission-description" + randomUuid());
    }
}
