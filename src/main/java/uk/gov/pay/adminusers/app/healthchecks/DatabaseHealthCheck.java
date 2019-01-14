package uk.gov.pay.adminusers.app.healthchecks;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import io.dropwizard.setup.Environment;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHealthCheck extends HealthCheck {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private static final Map<String, Long> longDatabaseStatsMap;
    private static final Map<String, Double> doubleDatabaseStatsMap;
    private Integer statsHealthy = 0;

    static {
        longDatabaseStatsMap = new HashMap<>();
        longDatabaseStatsMap.put("numbackends", 0L);
        longDatabaseStatsMap.put("xact_commit", 0L);
        longDatabaseStatsMap.put("xact_rollback", 0L);
        longDatabaseStatsMap.put("blks_read", 0L);
        longDatabaseStatsMap.put("blks_hit", 0L);
        longDatabaseStatsMap.put("tup_returned", 0L);
        longDatabaseStatsMap.put("tup_fetched", 0L);
        longDatabaseStatsMap.put("tup_inserted", 0L);
        longDatabaseStatsMap.put("tup_updated", 0L);
        longDatabaseStatsMap.put("tup_deleted", 0L);
        longDatabaseStatsMap.put("conflicts", 0L);
        longDatabaseStatsMap.put("temp_files", 0L);
        longDatabaseStatsMap.put("temp_bytes", 0L);
        longDatabaseStatsMap.put("deadlocks", 0L);
        doubleDatabaseStatsMap = new HashMap<>();
        doubleDatabaseStatsMap.put("blk_read_time", 0.0);
        doubleDatabaseStatsMap.put("blk_write_time", 0.0);
    }

    @Inject
    public DatabaseHealthCheck(AdminUsersConfig configuration, Environment environment) {
        this.dbUrl = configuration.getDataSourceFactory().getUrl();
        this.dbUser = configuration.getDataSourceFactory().getUser();
        this.dbPassword = configuration.getDataSourceFactory().getPassword();
        initialiseMetrics(environment.metrics());
    }

    private void initialiseMetrics(MetricRegistry metricRegistry) {
        for (String key : longDatabaseStatsMap.keySet()) {
            metricRegistry.<Gauge<Long>>register("adminusersdb." + key, () -> longDatabaseStatsMap.get(key));
        }
        for (String key : doubleDatabaseStatsMap.keySet()) {
            metricRegistry.<Gauge<Double>>register("adminusersdb." + key, () -> doubleDatabaseStatsMap.get(key));
        }
        metricRegistry.<Gauge<Integer>>register("adminusersdb.stats_healthy", () -> statsHealthy);
    }

    @Override
    protected Result check() {
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            connection.setReadOnly(true);
            updateMetricData(connection);
            return connection.isValid(2) ? Result.healthy() : Result.unhealthy("Could not validate the DB connection.");
        } catch (Exception e) {
            return Result.unhealthy(e.getMessage());
        }
    }

    private void updateMetricData(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("select * from pg_stat_database where datname='adminusers';");
            try (ResultSet resultSet = statement.getResultSet()) {
                resultSet.next();
                for (String key : longDatabaseStatsMap.keySet()) {
                    longDatabaseStatsMap.put(key, resultSet.getLong(key));
                }
                for (String key : doubleDatabaseStatsMap.keySet()) {
                    doubleDatabaseStatsMap.put(key, resultSet.getDouble(key));
                }
            }
            statsHealthy = 1;
        } catch (SQLException e) {
            statsHealthy = 0;
        }
    }
}
