package uk.gov.pay.adminusers.app.config;

import io.dropwizard.Configuration;

public class JPAConfiguration extends Configuration {

    private String jpaLoggingLevel;
    private String sqlLoggingLevel;
    private String ddlGenerationOutputMode;
    private String queryResultsCache;
    private String cacheSharedDefault;

    public String getJpaLoggingLevel() {
        return jpaLoggingLevel;
    }

    public String getSqlLoggingLevel() {
        return sqlLoggingLevel;
    }

    public String getDdlGenerationOutputMode() {
        return ddlGenerationOutputMode;
    }

    public String getQueryResultsCache() {
        return queryResultsCache;
    }

    public String getCacheSharedDefault() {
        return cacheSharedDefault;
    }
}
