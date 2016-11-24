package uk.gov.pay.adminusers.app.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseHealthCheck extends HealthCheck {

    private AdminUsersConfig configuration;

    @Inject
    public DatabaseHealthCheck(AdminUsersConfig configuration) {
        this.configuration = configuration;
    }

    @Override
    protected Result check() throws Exception {
        Connection connection = null;
        try {
            //TODO: disabling till the next pull request, until the AWS DB environments are ready
//            connection = DriverManager.getConnection(
//                configuration.getDataSourceFactory().getUrl(),
//                configuration.getDataSourceFactory().getUser(),
//                configuration.getDataSourceFactory().getPassword());
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
