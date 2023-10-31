package uk.gov.pay.adminusers.expungeandarchive.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.ExpungeAndArchiveDataConfig;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static ch.qos.logback.classic.Level.INFO;
import static java.time.ZoneOffset.UTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpungeAndArchiveHistoricalDataServiceTest {

    @Mock
    UserDao mockUserDao;

    @Mock
    InviteDao mockInviteDao;

    @Mock
    ForgottenPasswordDao mockForgottenPasswordDao;

    ExpungeAndArchiveHistoricalDataService expungeAndArchiveHistoricalDataService;

    @Mock
    AdminUsersConfig mockAdminUsersConfig;

    @Mock
    ExpungeAndArchiveDataConfig mockExpungeAndArchiveConfig;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;

    private final CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;

    String SYSTEM_INSTANT = "2022-03-03T10:15:30Z";
    Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse(SYSTEM_INSTANT), UTC);
        when(mockAdminUsersConfig.getExpungeAndArchiveDataConfig()).thenReturn(mockExpungeAndArchiveConfig);
        expungeAndArchiveHistoricalDataService = new ExpungeAndArchiveHistoricalDataService(mockUserDao,
                mockInviteDao, mockForgottenPasswordDao, mockAdminUsersConfig, clock);
    }

    @Test
    void shouldNotDeleteHistoricalDataIfFlagIsNotEnabled() {
        Logger root = (Logger) LoggerFactory.getLogger(ExpungeAndArchiveHistoricalDataService.class);
        root.addAppender(mockAppender);
        root.setLevel(INFO);

        when(mockExpungeAndArchiveConfig.isExpungeAndArchiveHistoricalDataEnabled()).thenReturn(false);
        expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

        verifyNoInteractions(mockUserDao);
        verifyNoInteractions(mockInviteDao);
        verifyNoInteractions(mockForgottenPasswordDao);

        verify(mockAppender).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(loggingEvents.get(0).getFormattedMessage(), is("Expunging and archiving historical data is not enabled"));
    }

    @Test
    void shouldDeleteHistoricalDataIfFlagIsEnabled() {
        Logger root = (Logger) LoggerFactory.getLogger(ExpungeAndArchiveHistoricalDataService.class);
        root.addAppender(mockAppender);
        root.setLevel(INFO);

        when(mockExpungeAndArchiveConfig.isExpungeAndArchiveHistoricalDataEnabled()).thenReturn(true);

        expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

        verify(mockUserDao).deleteUsersNotAssociatedWithAnyService(clock.instant());
        verify(mockInviteDao).deleteInvites(clock.instant().atZone(UTC));
        verify(mockForgottenPasswordDao).deleteForgottenPasswords(clock.instant().atZone(UTC));

        verify(mockAppender).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(loggingEvents.get(0).getFormattedMessage(), is("Completed expunging and archiving historical data"));
    }

    @Test
    void shouldObserveJobDurationForMetrics() {
        Double initialDuration = Optional.ofNullable(collectorRegistry.getSampleValue("expunge_and_archive_historical_data_job_duration_seconds_sum")).orElse(0.0);

        expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

        Double duration = collectorRegistry.getSampleValue("expunge_and_archive_historical_data_job_duration_seconds_sum");
        assertThat(duration, greaterThan(initialDuration));
    }
}
