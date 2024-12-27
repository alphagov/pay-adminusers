package uk.gov.pay.adminusers.app.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.persist.jpa.JpaPersistModule;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import uk.gov.pay.adminusers.app.RestClientFactory;
import uk.gov.pay.adminusers.resources.ResetPasswordValidator;
import uk.gov.pay.adminusers.resources.UserRequestValidator;
import uk.gov.pay.adminusers.service.ExistingUserOtpDispatcher;
import uk.gov.pay.adminusers.service.ForgottenPasswordServices;
import uk.gov.pay.adminusers.service.InviteServiceFactory;
import uk.gov.pay.adminusers.service.LinksBuilder;
import uk.gov.pay.adminusers.service.NotificationService;
import uk.gov.pay.adminusers.service.NotifyClientProvider;
import uk.gov.pay.adminusers.service.PasswordHasher;
import uk.gov.pay.adminusers.service.ResetPasswordService;
import uk.gov.pay.adminusers.service.SecondFactorAuthenticator;
import uk.gov.pay.adminusers.service.ServiceServicesFactory;
import uk.gov.pay.adminusers.service.UserServices;
import uk.gov.pay.adminusers.service.UserServicesFactory;
import uk.gov.pay.adminusers.utils.CountryConverter;
import uk.gov.pay.adminusers.validations.RequestValidations;
import uk.gov.service.payments.commons.queue.sqs.SqsQueueService;

import jakarta.ws.rs.client.Client;
import java.time.InstantSource;
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
        bind(InstantSource.class).toInstance(InstantSource.system());

        bind(PasswordHasher.class).in(Singleton.class);
        bind(CountryConverter.class).in(Singleton.class);
        bind(RequestValidations.class).in(Singleton.class);
        bind(UserRequestValidator.class).in(Singleton.class);
        bind(ResetPasswordValidator.class).in(Singleton.class);
        bind(Integer.class).annotatedWith(Names.named("LOGIN_ATTEMPT_CAP")).toInstance(configuration.getLoginAttemptCap());
        bind(SecondFactorAuthenticator.class).in(Singleton.class);
        bind(UserServices.class).in(Singleton.class);
        bind(ExistingUserOtpDispatcher.class).in(Singleton.class);
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
        properties.put("jakarta.persistence.jdbc.driver", dbConfig.getDriverClass());
        properties.put("jakarta.persistence.jdbc.url", dbConfig.getUrl());
        properties.put("jakarta.persistence.jdbc.user", dbConfig.getUser());
        properties.put("jakarta.persistence.jdbc.password", dbConfig.getPassword());

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
                new NotifyClientProvider(configuration.getNotifyConfiguration()),
                configuration.getNotifyConfiguration(),
                configuration.getNotifyDirectDebitConfiguration(),
                environment.metrics());
    }

    @Provides
    public ObjectMapper provideObjectMapper() {
        return environment.getObjectMapper();
    }

    @Provides
    public AmazonSQS sqsClient(AdminUsersConfig adminUsersConfig) {
        AmazonSQSClientBuilder clientBuilder = AmazonSQSClientBuilder
                .standard();

        if (adminUsersConfig.getSqsConfig().isNonStandardServiceEndpoint()) {

            BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(
                    adminUsersConfig.getSqsConfig().getAccessKey(),
                    adminUsersConfig.getSqsConfig().getSecretKey());

            clientBuilder
                    .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                    .withEndpointConfiguration(
                            new AwsClientBuilder.EndpointConfiguration(
                                    adminUsersConfig.getSqsConfig().getEndpoint(),
                                    adminUsersConfig.getSqsConfig().getRegion())
                    );
        } else {
            // uses AWS SDK's DefaultAWSCredentialsProviderChain to obtain credentials
            clientBuilder.withRegion(adminUsersConfig.getSqsConfig().getRegion());
        }

        return clientBuilder.build();
    }

    @Provides
    public SqsQueueService provideSqsQueueService(AmazonSQS amazonSQS, AdminUsersConfig adminUsersConfig) {
        return new SqsQueueService(
                amazonSQS,
                adminUsersConfig.getSqsConfig().getMessageMaximumWaitTimeInSeconds(),
                adminUsersConfig.getSqsConfig().getMessageMaximumBatchSize());
    }

    @Provides
    @jakarta.inject.Singleton
    public Client provideClient() {
        return RestClientFactory.buildClient(configuration.getRestClientConfig());
    }

}
