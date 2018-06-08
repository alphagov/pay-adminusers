package uk.gov.pay.adminusers.app.util;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Namespace;
import com.amazonaws.xray.entities.Subsegment;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.sessions.Record;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.SessionProfiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;

public class XRaySessionProfiler implements SessionProfiler {
    private int profileWeight = SessionProfiler.ALL;
    private static final Logger logger = LoggerFactory.getLogger(XRaySessionProfiler.class);
    private final AWSXRayRecorder recorder = AWSXRay.getGlobalRecorder();

    @Override
    public void endOperationProfile(String operationName) {
    }

    @Override
    public void endOperationProfile(String operationName, DatabaseQuery databaseQuery, int weight) {
    }

    @Override
    public Object profileExecutionOfQuery(DatabaseQuery databaseQuery, Record record, AbstractSession abstractSession) {

        HashMap additionalParams = new HashMap();
        DatabaseMetaData metadata;
        String hostname = "database";
        try {
            Connection connection = abstractSession.getAccessor().getConnection();
            metadata = connection.getMetaData();
            additionalParams.put("url", metadata.getURL());
            additionalParams.put("user", metadata.getUserName());
            additionalParams.put("driver_version", metadata.getDriverVersion());
            additionalParams.put("database_type", metadata.getDatabaseProductName());
            additionalParams.put("database_version", metadata.getDatabaseProductVersion());
            additionalParams.put("preparation", databaseQuery.isCallQuery() ? "call" : "statement");
            additionalParams.put("sanitized_query", StringUtils.isEmpty(databaseQuery.getSQLString()) ? "" : databaseQuery.getSQLString());

            try {
                hostname = new URI((new URI(metadata.getURL())).getSchemeSpecificPart()).getHost();
                hostname = connection.getCatalog() + "@" + hostname;
            } catch (URISyntaxException exception) {
                logger.warn("Error parsing database host name.");
            }
        } catch (SQLException exception) {
            logger.warn("Error getting database connection details.");
        }

        Subsegment subsegment = recorder.beginSubsegment(hostname);
        subsegment.putMetadata("monitor_name", databaseQuery.getMonitorName());
        subsegment.putMetadata("calling_class", databaseQuery.getClass().getSimpleName());
        subsegment.setNamespace(Namespace.REMOTE.toString());
        subsegment.putAllSql(additionalParams);

        try {
            return abstractSession.internalExecuteQuery(databaseQuery, (AbstractRecord) record);
        } finally {
            subsegment.end();
        }
    }

    @Override
    public void setSession(Session session) {
    }

    @Override
    public void startOperationProfile(String operationName) {
    }

    @Override
    public void startOperationProfile(String operationName, DatabaseQuery databaseQuery, int weight) {
    }

    @Override
    public void update(String operationName, Object value) {
    }

    @Override
    public void occurred(String operationName, AbstractSession abstractSession) {
    }

    @Override
    public void occurred(String operationName, DatabaseQuery databaseQuery, AbstractSession abstractSession) {
    }

    @Override
    public void setProfileWeight(int profileWeight) {
        this.profileWeight = profileWeight;
    }

    @Override
    public int getProfileWeight() {
        return this.profileWeight;
    }

    @Override
    public void initialize() {
    }
}
