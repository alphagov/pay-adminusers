package uk.gov.pay.adminusers.app.config;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

//TODO: disabling till the next pull request, until the AWS DB environments are ready
public class AdminUsersConfig extends Configuration{

//    @Valid
//    @NotNull
//    private DataSourceFactory dataSourceFactory;
//
//    @Valid
//    @NotNull
//    private JPAConfiguration jpaConfiguration;
//
//    public DataSourceFactory getDataSourceFactory() {
//        return dataSourceFactory;
//    }
//
//    public JPAConfiguration getJpaConfiguration() {
//        return jpaConfiguration;
//    }
}
