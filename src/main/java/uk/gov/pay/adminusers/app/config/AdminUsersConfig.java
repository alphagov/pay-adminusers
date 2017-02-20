package uk.gov.pay.adminusers.app.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.sql.*;

import static java.lang.String.format;


public class AdminUsersConfig extends Configuration {

    private static final Logger logger = PayLoggerFactory.getLogger(AdminUsersConfig.class);

    @Valid
    @NotNull
    private DataSourceFactory oldDataSourceFactory;

    @Valid
    @NotNull
    private DataSourceFactory newDataSourceFactory;

    @Valid
    @NotNull
    private JPAConfiguration jpaConfiguration;

    @NotNull
    private String graphiteHost;
    @NotNull
    private String graphitePort;

    @NotNull
    private String baseUrl;

    @NotNull
    private Integer loginAttemptCap;

    @NotNull
    private NotifyConfiguration notifyConfiguration;

    @NotNull
    private Integer timeStepsInSeconds;

    @NotNull
    private  ProxyConfiguration proxyConfiguration;

    @JsonProperty("proxy")
    public ProxyConfiguration getProxyConfiguration() {
        return proxyConfiguration;
    }

    public DataSourceFactory getDataSourceFactory() {
        //temporary switch to check if adminusers database exists and can be used
        if (shouldUseAdminUsersDatasource(getNewDataSourceFactory())) {
            return getNewDataSourceFactory();
        }
        return getOldDataSourceFactory();
    }

    @JsonProperty("database")
    public DataSourceFactory getOldDataSourceFactory() {
        return oldDataSourceFactory;
    }

    /**
     * TODO: rename this annotation to `database` when we remove the above `getOldDataSourceFactory`
     * @return
     */
    @JsonProperty("databaseNew")
    public DataSourceFactory getNewDataSourceFactory() {
        return newDataSourceFactory;
    }

    @JsonProperty("jpa")
    public JPAConfiguration getJpaConfiguration() {
        return jpaConfiguration;
    }

    public String getGraphiteHost() {
        return graphiteHost;
    }

    public String getGraphitePort() {
        return graphitePort;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Integer getLoginAttemptCap() {
        return loginAttemptCap;
    }

    @JsonProperty("notify")
    public NotifyConfiguration getNotifyConfiguration() {
        return notifyConfiguration;
    }

    public int getTimeStepsInSeconds() {
        return timeStepsInSeconds;
    }

    /**
     * Temporary method which checks availability of adminusers database, or switch to selfservice if not available
     * @param dataSourceFactory
     * @return
     */
    private boolean shouldUseAdminUsersDatasource(DataSourceFactory dataSourceFactory) {
        final String adminusersDb = "adminusers";
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(
                    dataSourceFactory.getUrl(),
                    dataSourceFactory.getUser(),
                    dataSourceFactory.getPassword());
            connection.setReadOnly(true);
            if (connection.isValid(2)) {
                PreparedStatement statement = connection.prepareStatement("select exists (select * from pg_tables where tablename='users')");
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    boolean usersExists = resultSet.getBoolean(1);
                    if (usersExists) {
                        logger.info("datasource {} is available and USERS table exists. Assuming its ready to be used", adminusersDb);
                        return true;
                    } else {
                        logger.info("datasource {} is available and USERS table does not exist. Switching to selfservice DB", adminusersDb);
                    }
                } else {
                    logger.error("datasource {} is available. But pg_tables doesn't seem to exists. Check database setup", adminusersDb);
                }
            } else {
                logger.warn("database {} is contactable. But connections cannot be made", adminusersDb);
            }
            return false;
        } catch (Exception e) {
            logger.info(format("datasource %s is not available > switching to selfservice database", adminusersDb), e);
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error("error closing test connection to " + adminusersDb, e);
                }
            }
        }
    }
}
