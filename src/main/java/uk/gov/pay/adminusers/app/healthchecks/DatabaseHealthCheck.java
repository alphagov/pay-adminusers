package uk.gov.pay.adminusers.app.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseHealthCheck extends HealthCheck {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    @Inject
    public DatabaseHealthCheck(AdminUsersConfig configuration) {
        this.dbUrl = configuration.getDataSourceFactory().getUrl();
        this.dbUser = configuration.getDataSourceFactory().getUser();
        this.dbPassword = configuration.getDataSourceFactory().getPassword();
    }

    @Override
    protected Result check() throws Exception {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            connection.setReadOnly(true);
            return connection.isValid(2) ? Result.healthy() : Result.unhealthy("Could not validate the DB connection.");
        } catch (Exception e) {
            return Result.unhealthy(e.getMessage());
        } finally {
            if (connection !=null) {
                connection.close();
            }
        }
    }

}
