package uk.gov.pay.adminusers.app.healthchecks;

import uk.gov.pay.adminusers.app.config.AdminUsersConfig;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ApplicationStartupDependentResource {

    private final AdminUsersConfig configuration;

    @Inject
    public ApplicationStartupDependentResource(AdminUsersConfig configuration) {
        this.configuration = configuration;
    }

    public Connection getDatabaseConnection() throws SQLException {
        //TODO: disabling till the next pull request, until the AWS DB environments are ready
//        return DriverManager.getConnection(
//                configuration.getDataSourceFactory().getUrl(),
//                configuration.getDataSourceFactory().getUser(),
//                configuration.getDataSourceFactory().getPassword());
        return null;
    }

    public void sleep(long durationSeconds) {
        try {
            Thread.sleep(durationSeconds);
        } catch (InterruptedException ignored) {}
    }

}
