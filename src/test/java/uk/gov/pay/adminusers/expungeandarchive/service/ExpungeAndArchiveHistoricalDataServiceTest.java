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
import uk.gov.pay.adminusers.client.ledger.exception.LedgerException;
import uk.gov.pay.adminusers.client.ledger.model.LedgerSearchTransactionsResponse;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;
import uk.gov.pay.adminusers.client.ledger.service.LedgerService;
import uk.gov.pay.adminusers.fixtures.ServiceEntityFixture;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceRoleDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.GatewayAccountIdEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.queue.ConnectorTaskQueue;
import uk.gov.pay.adminusers.queue.model.ConnectorTask;
import uk.gov.pay.adminusers.queue.model.ServiceArchivedTaskData;

import java.time.Instant;
import java.time.InstantSource;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ch.qos.logback.classic.Level.INFO;
import static java.time.ZoneOffset.UTC;
import static jakarta.ws.rs.core.Response.serverError;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.LedgerSearchTransactionsResponseFixture.aLedgerSearchTransactionsResponseFixture;
import static uk.gov.pay.adminusers.fixtures.LedgerTransactionFixture.aLedgerTransactionFixture;
import static uk.gov.pay.adminusers.fixtures.UserEntityFixture.aUserEntity;

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
    ConnectorTaskQueue mockConnectorTaskQueue;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;

    private final CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;

    String SYSTEM_INSTANT = "2022-03-03T10:15:30Z";
    InstantSource instantSource;

    @BeforeEach
    void setUp() {
        instantSource = InstantSource.fixed(Instant.parse(SYSTEM_INSTANT));
        when(mockAdminUsersConfig.getExpungeAndArchiveDataConfig()).thenReturn(mockExpungeAndArchiveConfig);
        expungeAndArchiveHistoricalDataService = new ExpungeAndArchiveHistoricalDataService(mockUserDao,
                mockInviteDao, mockForgottenPasswordDao, mockServiceDao, mockServiceRoleDao, mockLedgerService,
                mockAdminUsersConfig, mockConnectorTaskQueue, instantSource);
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

        verify(mockUserDao).deleteUsersNotAssociatedWithAnyService(instantSource.instant());
        verify(mockInviteDao).deleteInvites(instantSource.instant().atZone(UTC));
        verify(mockForgottenPasswordDao).deleteForgottenPasswords(instantSource.instant().atZone(UTC));

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

        RoleEntity adminRole = new RoleEntity(new Role(2, RoleName.ADMIN, "Administrator"));
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

            systemDate = instantSource.instant().atZone(UTC);
        }

        @Test
        void shouldSendEventToConnectorTasksQueueWhenServiceIsArchived() {
            shouldArchiveService_WhenTheLastTransactionDateIsBeforeTheServicesEligibleForArchivingDate();
            verify(mockConnectorTaskQueue).addTaskToQueue(new ConnectorTask(new ServiceArchivedTaskData(serviceEntity.getExternalId()), "service_archived"));
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
            assertThat(serviceEntity.getArchivedDate(), is(instantSource.instant().atZone(UTC)));
            verify(mockServiceDao).merge(serviceEntity);
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
            assertThat(serviceEntity.getArchivedDate(), is(instantSource.instant().atZone(UTC)));
            verify(mockServiceDao).merge(serviceEntity);
        }

        @Test
        void shouldArchiveServiceWithFirstCheckedForArchivalDateBeforeServiceArchivalDate() {
            when(mockExpungeAndArchiveConfig.getArchiveServicesAfterDays()).thenReturn(7);

            LedgerSearchTransactionsResponse searchTransactionsResponse = aLedgerSearchTransactionsResponseFixture()
                    .withTransactionList(List.of())
                    .build();
            when(mockLedgerService.searchTransactions(gatewayAccountId1, 1)).thenReturn(searchTransactionsResponse);

            serviceEntity = ServiceEntityFixture
                    .aServiceEntity()
                    .withArchived(false)
                    .withCreatedDate(null)
                    .withFirstCheckedForArchivalDate(systemDate.minusDays(8))
                    .withGatewayAccounts(List.of(gatewayAccountIdEntity1))
                    .build();
            when(mockServiceDao.findServicesToCheckForArchiving(systemDate.minusDays(7))).thenReturn(List.of(serviceEntity));

            expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

            assertTrue(serviceEntity.isArchived());
            assertThat(serviceEntity.getArchivedDate(), is(instantSource.instant().atZone(UTC)));
            verify(mockServiceDao).merge(serviceEntity);
        }

        @Test
        void shouldRemoveUsersFromServiceWhenServiceIsArchived() {
            Logger root = (Logger) LoggerFactory.getLogger(ExpungeAndArchiveHistoricalDataService.class);
            root.addAppender(mockAppender);
            root.setLevel(INFO);
            
            when(mockExpungeAndArchiveConfig.getArchiveServicesAfterDays()).thenReturn(7);
            when(mockExpungeAndArchiveConfig.getArchiveServicesAfterDays()).thenReturn(7);

            LedgerTransaction ledgerTransaction = aLedgerTransactionFixture()
                    .withCreatedDate(systemDate.minusDays(10))
                    .build();
            LedgerSearchTransactionsResponse searchTransactionsResponse = aLedgerSearchTransactionsResponseFixture()
                    .withTransactionList(List.of(ledgerTransaction))
                    .build();

            when(mockLedgerService.searchTransactions(gatewayAccountId1, 1)).thenReturn(searchTransactionsResponse);
            when(mockServiceDao.findServicesToCheckForArchiving(systemDate.minusDays(7))).thenReturn(List.of(serviceEntity));

            ServiceRoleEntity serviceRoleEntity = new ServiceRoleEntity(serviceEntity, adminRole);
            serviceRoleEntity.setUser(aUserEntity().build());
            when(mockServiceRoleDao.findServiceUserRoles(serviceEntity.getId())).thenReturn(List.of(serviceRoleEntity));

            expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

            assertTrue(serviceEntity.isArchived());
            verify(mockServiceDao).merge(serviceEntity);
            verify(mockServiceRoleDao).remove(serviceRoleEntity);

            verify(mockAppender, times(3)).doAppend(loggingEventArgumentCaptor.capture());
            List<LoggingEvent> loggingEvents = loggingEventArgumentCaptor.getAllValues();
            assertThat(loggingEvents.stream().map(LoggingEvent::getFormattedMessage).collect(Collectors.toList()), 
                    hasItems("Removed user from service"));
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
            assertThat(serviceEntity.getFirstCheckedForArchivalDate(), is(systemDate.withZoneSameInstant(UTC)));
            assertThat(serviceEntity.getSkipCheckingForArchivalUntilDate(), is(systemDate.plusDays(8)));
            verify(mockServiceDao).merge(serviceEntity);
            verifyNoInteractions(mockServiceRoleDao);
        }

        @Test
        void shouldNotArchiveService_WhenTheFirstCheckedForArchivalDateIsAfterTheServicesEligibleForArchivingDate() {
            when(mockExpungeAndArchiveConfig.getArchiveServicesAfterDays()).thenReturn(7);

            LedgerSearchTransactionsResponse searchTransactionsResponse = aLedgerSearchTransactionsResponseFixture()
                    .withTransactionList(List.of())
                    .build();

            serviceEntity = ServiceEntityFixture
                    .aServiceEntity()
                    .withArchived(false)
                    .withCreatedDate(null)
                    .withFirstCheckedForArchivalDate(systemDate)
                    .withGatewayAccounts(List.of(gatewayAccountIdEntity1))
                    .build();

            when(mockLedgerService.searchTransactions(gatewayAccountId1, 1)).thenReturn(searchTransactionsResponse);
            when(mockServiceDao.findServicesToCheckForArchiving(systemDate.minusDays(7))).thenReturn(List.of(serviceEntity));

            expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

            assertFalse(serviceEntity.isArchived());
            assertThat(serviceEntity.getFirstCheckedForArchivalDate(), is(systemDate));
            assertThat(serviceEntity.getSkipCheckingForArchivalUntilDate(), is(systemDate.plusDays(7)));
            verify(mockServiceDao).merge(serviceEntity);
            verifyNoInteractions(mockServiceRoleDao);
        }

        @Test
        void shouldNotSetFirstCheckedForArchivalDateIfItIsAlreadySet() {
            when(mockExpungeAndArchiveConfig.getArchiveServicesAfterDays()).thenReturn(7);
            serviceEntity.setFirstCheckedForArchivalDate(ZonedDateTime.parse("2021-01-01T10:15:30Z"));

            LedgerTransaction ledgerTransaction = aLedgerTransactionFixture()
                    .withCreatedDate(systemDate)
                    .build();
            LedgerSearchTransactionsResponse searchTransactionsResponse = aLedgerSearchTransactionsResponseFixture()
                    .withTransactionList(List.of(ledgerTransaction))
                    .build();

            when(mockLedgerService.searchTransactions(gatewayAccountId1, 1)).thenReturn(searchTransactionsResponse);
            when(mockServiceDao.findServicesToCheckForArchiving(systemDate.minusDays(7))).thenReturn(List.of(serviceEntity));

            expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

            assertFalse(serviceEntity.isArchived());
            assertThat(serviceEntity.getFirstCheckedForArchivalDate().toString(), is("2021-01-01T10:15:30Z"));
            verify(mockServiceDao).merge(serviceEntity);
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
            assertThat(serviceEntity.getSkipCheckingForArchivalUntilDate(), is(systemDate.plusDays(7)));
            verify(mockServiceDao).merge(serviceEntity);
            verifyNoInteractions(mockServiceRoleDao);
        }

        @Test
        void shouldNotArchiveService_WhenLedgerReturnsNoTransactions_AndCreatedDateOrFirstCheckedForArchivalDateAreNotAvailable() {
            when(mockExpungeAndArchiveConfig.getArchiveServicesAfterDays()).thenReturn(7);

            LedgerSearchTransactionsResponse searchTransactionsResponse = aLedgerSearchTransactionsResponseFixture()
                    .withTransactionList(List.of())
                    .build();
            when(mockLedgerService.searchTransactions(gatewayAccountId1, 1)).thenReturn(searchTransactionsResponse);

            serviceEntity = ServiceEntityFixture
                    .aServiceEntity()
                    .withArchived(false)
                    .withCreatedDate(null)
                    .withFirstCheckedForArchivalDate(null)
                    .withGatewayAccounts(List.of(gatewayAccountIdEntity1))
                    .build();
            when(mockServiceDao.findServicesToCheckForArchiving(systemDate.minusDays(7))).thenReturn(List.of(serviceEntity));

            expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

            assertFalse(serviceEntity.isArchived());
            assertThat(serviceEntity.getFirstCheckedForArchivalDate(), is(systemDate.withZoneSameInstant(UTC)));
            assertThat(serviceEntity.getSkipCheckingForArchivalUntilDate(), is(systemDate.plusDays(7)));
            verify(mockServiceDao).merge(serviceEntity);
            verifyNoInteractions(mockServiceRoleDao);
        }

        @Test
        void shouldLogAndContinueArchivingServicesForLedgerErrors() {
            when(mockExpungeAndArchiveConfig.getArchiveServicesAfterDays()).thenReturn(7);

            when(mockLedgerService.searchTransactions(gatewayAccountId1, 1)).thenThrow(new LedgerException(serverError().build()));
            serviceEntity = ServiceEntityFixture
                    .aServiceEntity()
                    .withArchived(false)
                    .withCreatedDate(null)
                    .withFirstCheckedForArchivalDate(null)
                    .withGatewayAccounts(List.of(gatewayAccountIdEntity1))
                    .build();

            LedgerSearchTransactionsResponse searchTransactionsResponse = aLedgerSearchTransactionsResponseFixture()
                    .withTransactionList(List.of())
                    .build();
            when(mockLedgerService.searchTransactions(gatewayAccountId2, 1)).thenReturn(searchTransactionsResponse);
            ServiceEntity serviceEntityToArchive = ServiceEntityFixture
                    .aServiceEntity()
                    .withArchived(false)
                    .withCreatedDate(systemDate.minusDays(10))
                    .withFirstCheckedForArchivalDate(null)
                    .withGatewayAccounts(List.of(gatewayAccountIdEntity2))
                    .build();
            when(mockServiceDao.findServicesToCheckForArchiving(systemDate.minusDays(7))).thenReturn(List.of(serviceEntity, serviceEntityToArchive));

            expungeAndArchiveHistoricalDataService.expungeAndArchiveHistoricalData();

            assertFalse(serviceEntity.isArchived());
            assertThat(serviceEntity.getFirstCheckedForArchivalDate(), is(nullValue()));
            assertThat(serviceEntity.getSkipCheckingForArchivalUntilDate(), is(nullValue()));

            assertTrue(serviceEntityToArchive.isArchived());

            verify(mockServiceDao).merge(serviceEntityToArchive);
            verifyNoMoreInteractions(mockServiceDao);
        }
    }
}
