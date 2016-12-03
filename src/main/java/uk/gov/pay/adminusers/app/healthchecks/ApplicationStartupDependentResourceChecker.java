package uk.gov.pay.adminusers.app.healthchecks;

import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class ApplicationStartupDependentResourceChecker {

    private static final int PROGRESSIVE_SECONDS_TO_WAIT = 5;
    private static final Logger logger = PayLoggerFactory.getLogger(ApplicationStartupDependentResourceChecker.class);

    private final ApplicationStartupDependentResource applicationStartupDependentResource;

    public ApplicationStartupDependentResourceChecker(ApplicationStartupDependentResource applicationStartupDependentResource) {
        this.applicationStartupDependentResource = applicationStartupDependentResource;
    }

    public void checkAndWaitForResources() {
        waitingForDatabaseConnectivity();
    }

    private void waitingForDatabaseConnectivity() {
        logger.info("Checking for database availability >>>");
        boolean databaseAvailable = isDatabaseAvailable();

        long timeToWait = 0;
        while(!databaseAvailable) {
            timeToWait += PROGRESSIVE_SECONDS_TO_WAIT;
            logger.info("Waiting for "+ timeToWait +" seconds till the database is available ...");
            applicationStartupDependentResource.sleep(timeToWait * 1000);
            databaseAvailable = isDatabaseAvailable();
        }
        logger.info("Database available.");
    }


    private boolean isDatabaseAvailable() {
        Connection connection = null;
        try {
            connection = applicationStartupDependentResource.getDatabaseConnection();
            return true;
        } catch (SQLException e) {
            return false;
        } finally {
            if(connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Error closing connection acquired for Database check");
                }
            }
        }
    }
}
