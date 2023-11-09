package uk.gov.pay.adminusers.expungeandarchive.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.ExpungeAndArchiveDataConfig;
import uk.gov.pay.adminusers.client.ledger.model.LedgerSearchTransactionsResponse;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;
import uk.gov.pay.adminusers.client.ledger.service.LedgerService;
import uk.gov.pay.adminusers.fixtures.ServiceEntityFixture;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceRoleDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.GatewayAccountIdEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static ch.qos.logback.classic.Level.INFO;
import static java.time.ZoneOffset.UTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.LedgerSearchTransactionsResponseFixture.aLedgerSearchTransactionsResponseFixture;
import static uk.gov.pay.adminusers.fixtures.LedgerTransactionFixture.aLedgerTransactionFixture;

@ExtendWith(MockitoExtension.class)
class ExpungeAndArchiveHistoricalDataServiceTest {

    @Mock
    UserDao mockUserDao;

    @Mock
    InviteDao mockInviteDao;

    @Mock
    ServiceDao mockServiceDao;

    @Mock
    ServiceRoleDao mockServiceRoleDao;

    @Mock
    LedgerService mockLedgerService;

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
                mockInviteDao, mockForgottenPasswordDao, mockServiceDao, mockServiceRoleDao, mockLedgerService, mockAdminUsersConfig, clock);
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
        verifyNoInteractions(mockServiceDao);

        verify(mockAppender).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
        assertThat(loggingEvents.get(0).getFormattedMessage(), is("Expunging and archiving historical data is not enabled"));
    }

    @Test
    void shouldDeleteHistoricalUserDataIfFlagIsEnabled() {
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

    @Nested
    class TestArchivingServices {

        ServiceEntity serviceEntity;
        String gatewayAccountId1 = randomUuid();
        String gatewayAccountId2 = randomUuid();
        ZonedDateTime systemDate;

        GatewayAccountIdEntity gatewayAccountIdEntity1;
        GatewayAccountIdEntity gatewayAccountIdEntity2;

        @BeforeEach
        void setUp() {
            when(mockExpungeAndArchiveConfig.isExpungeAndArchiveHistoricalDataEnabled()).thenReturn(true);
            when(mockExpungeAndArchiveConfig.getArchiveServicesAfterDays()).thenReturn(7);

            gatewayAccountIdEntity1 = new GatewayAccountIdEntity();
            gatewayAccountIdEntity1.setGatewayAccountId(gatewayAccountId1);

            gatewayAccountIdEntity2 = new GatewayAccountIdEntity();
            gatewayAccountIdEntity2.setGatewayAccountId(gatewayAccountId2);

            serviceEntity = ServiceEntityFixture
                    .aServiceEntity()
                    .withArchived(false)
                    .withGatewayAccounts(List.of(gatewayAccountIdEntity1, gatewayAccountIdEntity2))
                    .build();

            systemDate = clock.instant().atZone(UTC);
        }

        @Test
        void shouldArchiveService_WhenTheLastTransactionDateIsBeforeTheServicesEligibleForArchivingDate() {
            when(mockExpungeAndArchiveConfig.getArchiveServicesAfterDays()).thenReturn(7);

            LedgerTransaction ledgerTransaction1 = aLedgerTransactionFixture()
                    .withCreatedDate(systemDate.minusDays(10))
                    .build();
            LedgerSearchTransactionsResponse searchTransactionsResponse1 = aLedgerSearchTransactionsResponseFixture()
                    .withTransactionList(List.of(ledgerTransaction1))
                    .build();

            LedgerTransaction ledgerTransaction2 = aLedgerTransactionFixture()
                    .withCreatedDate(systemDate.minusDays(20))
                    .build();
            LedgerSearchTransactionsResponse searchTransactionsResponse2 = aLedgerSearchTransactionsResponseFixture()
                    .withTransactionList(List.of(ledgerTransaction2))
                    .build();

            when(mockLedgerService.searchTransactions(gatewayAccountId1, 1)).thenReturn(searchTransactionsResponse1);
            when(mockLedgerService.searchTransactions(gatewayAccountId2, 1)).thenReturn(searchTransactionsResponse2);
            when(mockServiceDao.findServicesToCheckForArchiving(systemDate.minusDays(7))).thenReturn(List.of(serviceEntity));

            expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

            assertTrue(serviceEntity.isArchived());
            assertThat(serviceEntity.getArchivedDate(), is(clock.instant().atZone(UTC)));
            verify(mockServiceDao).merge(serviceEntity);
            verify(mockServiceRoleDao).removeUsersFromService(serviceEntity.getId());
        }

        @Test
        void shouldArchiveServiceWithoutTransactionsButCreatedBeforeTheServicesEligibleForArchivingDate() {
            when(mockExpungeAndArchiveConfig.getArchiveServicesAfterDays()).thenReturn(7);

            LedgerSearchTransactionsResponse searchTransactionsResponse1 = aLedgerSearchTransactionsResponseFixture()
                    .withTransactionList(List.of())
                    .build();
            when(mockLedgerService.searchTransactions(gatewayAccountId1, 1)).thenReturn(searchTransactionsResponse1);

            serviceEntity = ServiceEntityFixture
                    .aServiceEntity()
                    .withArchived(false)
                    .withCreatedDate(systemDate.minusDays(8))
                    .withGatewayAccounts(List.of(gatewayAccountIdEntity1))
                    .build();
            when(mockServiceDao.findServicesToCheckForArchiving(systemDate.minusDays(7))).thenReturn(List.of(serviceEntity));

            expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

            assertTrue(serviceEntity.isArchived());
            assertThat(serviceEntity.getArchivedDate(), is(clock.instant().atZone(UTC)));
            verify(mockServiceDao).merge(serviceEntity);
            verify(mockServiceRoleDao).removeUsersFromService(serviceEntity.getId());
        }

        @Test
        void shouldNotArchiveService_WhenTheLastTransactionDateIsAfterTheServicesEligibleForArchivingDate() {
            when(mockExpungeAndArchiveConfig.getArchiveServicesAfterDays()).thenReturn(7);

            LedgerSearchTransactionsResponse searchTransactionsResponse1 = aLedgerSearchTransactionsResponseFixture()
                    .withTransactionList(List.of())
                    .build();

            LedgerTransaction ledgerTransaction2 = aLedgerTransactionFixture()
                    .withCreatedDate(systemDate.plusDays(1))
                    .build();
            LedgerSearchTransactionsResponse searchTransactionsResponse2 = aLedgerSearchTransactionsResponseFixture()
                    .withTransactionList(List.of(ledgerTransaction2))
                    .build();

            when(mockLedgerService.searchTransactions(gatewayAccountId1, 1)).thenReturn(searchTransactionsResponse1);
            when(mockLedgerService.searchTransactions(gatewayAccountId2, 1)).thenReturn(searchTransactionsResponse2);
            when(mockServiceDao.findServicesToCheckForArchiving(systemDate.minusDays(7))).thenReturn(List.of(serviceEntity));

            expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

            assertFalse(serviceEntity.isArchived());
            verifyNoMoreInteractions(mockServiceDao);
        }

        @Test
        void shouldNotArchiveServiceWithoutTransactionsAndCreatedIsAfterTheServicesEligibleForArchivingDate() {
            when(mockExpungeAndArchiveConfig.getArchiveServicesAfterDays()).thenReturn(7);

            LedgerSearchTransactionsResponse searchTransactionsResponse1 = aLedgerSearchTransactionsResponseFixture()
                    .withTransactionList(List.of())
                    .build();
            when(mockLedgerService.searchTransactions(gatewayAccountId1, 1)).thenReturn(searchTransactionsResponse1);

            serviceEntity = ServiceEntityFixture
                    .aServiceEntity()
                    .withArchived(false)
                    .withCreatedDate(systemDate)
                    .withGatewayAccounts(List.of(gatewayAccountIdEntity1))
                    .build();
            when(mockServiceDao.findServicesToCheckForArchiving(systemDate.minusDays(7))).thenReturn(List.of(serviceEntity));

            expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

            assertFalse(serviceEntity.isArchived());
            verifyNoMoreInteractions(mockServiceDao);
        }

        @Test
        void shouldNotArchiveServiceWhenLedgerReturnsNoTransactionsAndCreatedDateIsNotAvailableOnService() {
            when(mockExpungeAndArchiveConfig.getArchiveServicesAfterDays()).thenReturn(7);

            LedgerSearchTransactionsResponse searchTransactionsResponse1 = aLedgerSearchTransactionsResponseFixture()
                    .withTransactionList(List.of())
                    .build();
            when(mockLedgerService.searchTransactions(gatewayAccountId1, 1)).thenReturn(searchTransactionsResponse1);

            serviceEntity = ServiceEntityFixture
                    .aServiceEntity()
                    .withArchived(false)
                    .withCreatedDate(null)
                    .withGatewayAccounts(List.of(gatewayAccountIdEntity1))
                    .build();
            when(mockServiceDao.findServicesToCheckForArchiving(systemDate.minusDays(7))).thenReturn(List.of(serviceEntity));

            expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

            assertFalse(serviceEntity.isArchived());
            verifyNoMoreInteractions(mockServiceDao);
        }
    }
}
