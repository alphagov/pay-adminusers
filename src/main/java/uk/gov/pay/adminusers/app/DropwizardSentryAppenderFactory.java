package uk.gov.pay.adminusers.app;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import io.dropwizard.logging.common.AbstractAppenderFactory;
import io.dropwizard.logging.common.async.AsyncAppenderFactory;
import io.dropwizard.logging.common.filter.LevelFilterFactory;
import io.dropwizard.logging.common.layout.LayoutFactory;
import io.sentry.Sentry;
import io.sentry.SentryOptions;
import io.sentry.logback.SentryAppender;
import org.dhatim.dropwizard.sentry.SentryConfigurator;
import org.dhatim.dropwizard.sentry.filters.DroppingSentryLoggingFilter;

import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonTypeName("pay-dropwizard-sentry")
public class DropwizardSentryAppenderFactory extends AbstractAppenderFactory<ILoggingEvent> {

    @JsonProperty
    public @NotNull String dsn = null;
    @JsonProperty
    public String environment = null;
    @JsonProperty
    public Map<String, String> tags = null;
    @JsonProperty
    public String release = null;
    @JsonProperty
    public String serverName = null;
    @JsonProperty
    public List<String> inAppIncludes = null;
    @JsonProperty
    public List<String> inAppExcludes = null;
    @JsonProperty
    public String configurator = null;

    public DropwizardSentryAppenderFactory() {
    }

    @Override
    public Appender<ILoggingEvent> build(LoggerContext context, 
                                         String applicationName, 
                                         LayoutFactory<ILoggingEvent> layoutFactory, 
                                         LevelFilterFactory<ILoggingEvent> levelFilterFactory, 
                                         AsyncAppenderFactory<ILoggingEvent> asyncAppenderFactory) {
        Preconditions.checkNotNull(context);
        SentryOptions options = new SentryOptions();
        options.setDsn(this.dsn);
        Optional.ofNullable(this.environment).ifPresent(options::setEnvironment);
        Optional.ofNullable(this.tags).ifPresent((tags) -> {
            tags.forEach(options::setTag);
        });
        Optional.ofNullable(this.release).ifPresent(options::setRelease);
        Optional.ofNullable(this.serverName).ifPresent(options::setServerName);
        Optional.ofNullable(this.inAppIncludes).ifPresent((inAppIncludes) -> {
            inAppIncludes.forEach(options::addInAppInclude);
        });
        Optional.ofNullable(this.inAppExcludes).ifPresent((inAppExcludes) -> {
            inAppExcludes.forEach(options::addInAppExclude);
        });
        Optional.ofNullable(this.configurator).ifPresent((configurator) -> {
            try {
                Class<?> klass = Class.forName(configurator);
                if (!SentryConfigurator.class.isAssignableFrom(klass)) {
                    throw new IllegalArgumentException("configurator class " + configurator + " does not implement " + SentryConfigurator.class.getName());
                } else {
                    SentryConfigurator sentryConfigurator = (SentryConfigurator)klass.getDeclaredConstructor().newInstance();
                    sentryConfigurator.configure(options);
                }
            } catch (ClassNotFoundException var4) {
                throw new IllegalArgumentException("configurator class " + configurator + " not found", var4);
            } catch (NoSuchMethodException var5) {
                throw new IllegalArgumentException("configurator class " + configurator + " does not define a default constructor", var5);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException var6) {
                throw new IllegalArgumentException("cannot invoke default constructor on configurator class " + configurator, var6);
            }
        });
        Sentry.close();
        SentryAppender appender = new SentryAppender();
        appender.setOptions(options);
        appender.setName("dropwizard-sentry");
        appender.setContext(context);
        appender.setMinimumBreadcrumbLevel(this.threshold);
        appender.setMinimumEventLevel(this.threshold);
        appender.addFilter(levelFilterFactory.build(this.threshold));
        this.getFilterFactories().forEach((f) -> {
            appender.addFilter(f.build());
        });
        appender.start();
        Filter<ILoggingEvent> filter = new DroppingSentryLoggingFilter();
        filter.start();
        appender.addFilter(filter);
        return appender;
    }
}
