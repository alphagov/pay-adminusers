package uk.gov.pay.adminusers.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.persist.jpa.JpaPersistModule;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.setup.Environment;
import uk.gov.pay.adminusers.resources.ResetPasswordValidator;
import uk.gov.pay.adminusers.resources.UserRequestValidator;
import uk.gov.pay.adminusers.service.ForgottenPasswordServices;
import uk.gov.pay.adminusers.service.InviteServiceFactory;
import uk.gov.pay.adminusers.service.LinksBuilder;
import uk.gov.pay.adminusers.service.NotificationService;
import uk.gov.pay.adminusers.service.PasswordHasher;
import uk.gov.pay.adminusers.service.ResetPasswordService;
import uk.gov.pay.adminusers.service.SecondFactorAuthenticator;
import uk.gov.pay.adminusers.service.ServiceServicesFactory;
import uk.gov.pay.adminusers.service.UserServices;
import uk.gov.pay.adminusers.service.UserServicesFactory;
import uk.gov.pay.adminusers.utils.CountryConverter;
import uk.gov.pay.adminusers.validations.RequestValidations;

import java.time.Clock;
import java.util.Properties;

public class AdminUsersModule extends AbstractModule {

    private final AdminUsersConfig configuration;
    private final SecondFactorAuthConfiguration secondFactorAuthConfig;
    private final Environment environment;

    public AdminUsersModule(final AdminUsersConfig configuration, final Environment environment) {
        super();
        this.configuration = configuration;
        this.secondFactorAuthConfig = configuration.getSecondFactorAuthConfiguration();
        this.environment = environment;
    }

    @Override
    protected void configure() {
        bind(AdminUsersConfig.class).toInstance(configuration);
        bind(Environment.class).toInstance(environment);
        bind(LinksBuilder.class).toInstance(new LinksBuilder(configuration.getBaseUrl()));
        bind(GoogleAuthenticatorConfig.class).toInstance(new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setWindowSize(secondFactorAuthConfig.getValidTimeWindows())
                .setTimeStepSizeInMillis(secondFactorAuthConfig.getTimeWindowInMillis())
                .build());
        bind(LinksConfig.class).toInstance(configuration.getLinks());
        bind(Clock.class).toInstance(Clock.systemDefaultZone());

        bind(PasswordHasher.class).in(Singleton.class);
        bind(CountryConverter.class).in(Singleton.class);
        bind(RequestValidations.class).in(Singleton.class);
        bind(UserRequestValidator.class).in(Singleton.class);
        bind(ResetPasswordValidator.class).in(Singleton.class);
        bind(Integer.class).annotatedWith(Names.named("LOGIN_ATTEMPT_CAP")).toInstance(configuration.getLoginAttemptCap());
        bind(SecondFactorAuthenticator.class).in(Singleton.class);
        bind(UserServices.class).in(Singleton.class);
        bind(ForgottenPasswordServices.class).in(Singleton.class);
        bind(ResetPasswordService.class).in(Singleton.class);


        bind(Integer.class).annotatedWith(Names.named("FORGOTTEN_PASSWORD_EXPIRY_MINUTES")).toInstance(configuration.getForgottenPasswordExpiryMinutes());

        install(jpaModule(configuration));
        install(new FactoryModuleBuilder().build(UserServicesFactory.class));
        install(new FactoryModuleBuilder().build(ServiceServicesFactory.class));
        install(new FactoryModuleBuilder().build(InviteServiceFactory.class));

    }

    private JpaPersistModule jpaModule(AdminUsersConfig configuration) {
        DataSourceFactory dbConfig = configuration.getDataSourceFactory();
        final Properties properties = new Properties();
        properties.put("javax.persistence.jdbc.driver", dbConfig.getDriverClass());
        properties.put("javax.persistence.jdbc.url", dbConfig.getUrl());
        properties.put("javax.persistence.jdbc.user", dbConfig.getUser());
        properties.put("javax.persistence.jdbc.password", dbConfig.getPassword());

        JPAConfiguration jpaConfiguration = configuration.getJpaConfiguration();
        properties.put("eclipselink.logging.level", jpaConfiguration.getJpaLoggingLevel());
        properties.put("eclipselink.logging.level.sql", jpaConfiguration.getSqlLoggingLevel());
        properties.put("eclipselink.query-results-cache", jpaConfiguration.getCacheSharedDefault());
        properties.put("eclipselink.cache.shared.default", jpaConfiguration.getCacheSharedDefault());
        properties.put("eclipselink.ddl-generation.output-mode", jpaConfiguration.getDdlGenerationOutputMode());
        properties.put("eclipselink.session.customizer", "uk.gov.pay.adminusers.app.config.AdminUsersSessionCustomiser");

        final JpaPersistModule jpaModule = new JpaPersistModule("AdminUsersUnit");
        jpaModule.properties(properties);

        return jpaModule;
    }

    @Provides
    public NotificationService provideUserNotificationService() {
        return new NotificationService(
                configuration.getNotifyConfiguration(),
                configuration.getNotifyDirectDebitConfiguration(),
                environment.metrics());
    }

    @Provides
    public ObjectMapper provideObjectMapper() {
        return environment.getObjectMapper();
    }

}
