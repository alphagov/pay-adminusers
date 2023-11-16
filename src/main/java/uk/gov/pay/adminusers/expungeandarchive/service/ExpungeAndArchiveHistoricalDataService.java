package uk.gov.pay.adminusers.expungeandarchive.service;

import com.google.inject.Inject;
import io.prometheus.client.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.ExpungeAndArchiveDataConfig;
import uk.gov.pay.adminusers.client.ledger.model.LedgerSearchTransactionsResponse;
import uk.gov.pay.adminusers.client.ledger.service.LedgerService;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.ServiceRoleDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.queue.ConnectorTaskQueue;
import uk.gov.pay.adminusers.queue.model.ConnectorTask;
import uk.gov.pay.adminusers.queue.model.ServiceArchivedTaskData;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.service.payments.logging.LoggingKeys.SERVICE_EXTERNAL_ID;

public class ExpungeAndArchiveHistoricalDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpungeAndArchiveHistoricalDataService.class);
    private final UserDao userDao;
    private final InviteDao inviteDao;
    private final ForgottenPasswordDao forgottenPasswordDao;
    private final ServiceDao serviceDao;
    private final ServiceRoleDao serviceRoleDao;
    private final LedgerService ledgerService;
    private final ExpungeAndArchiveDataConfig expungeAndArchiveDataConfig;
    private final ConnectorTaskQueue connectorTaskQueue;
    private final Clock clock;

    private static final Histogram duration = Histogram.build()
            .name("expunge_and_archive_historical_data_job_duration_seconds")
            .help("Duration of expunge and archive historical data job in seconds")
            .unit("seconds")
            .register();

    @Inject
    public ExpungeAndArchiveHistoricalDataService(UserDao userDao, InviteDao inviteDao,
                                                  ForgottenPasswordDao forgottenPasswordDao,
                                                  ServiceDao serviceDao,
                                                  ServiceRoleDao serviceRoleDao,
                                                  LedgerService ledgerService,
                                                  AdminUsersConfig adminUsersConfig,
                                                  ConnectorTaskQueue connectorTaskQueue,
                                                  Clock clock) {
        this.userDao = userDao;
        this.inviteDao = inviteDao;
        this.forgottenPasswordDao = forgottenPasswordDao;
        this.serviceDao = serviceDao;
        this.serviceRoleDao = serviceRoleDao;
        this.ledgerService = ledgerService;
        this.expungeAndArchiveDataConfig = adminUsersConfig.getExpungeAndArchiveDataConfig();
        this.connectorTaskQueue = connectorTaskQueue;
        this.clock = clock;
    }

    public void expungeAndArchiveHistoricalData() {
        Histogram.Timer responseTimeTimer = duration.startTimer();

        try {
            if (expungeAndArchiveDataConfig.isExpungeAndArchiveHistoricalDataEnabled()) {
                ZonedDateTime deleteUsersAndRelatedDataBeforeDate = getDeleteUsersAndRelatedDataBeforeDate();

                int noOfInvitesDeleted = inviteDao.deleteInvites(deleteUsersAndRelatedDataBeforeDate);
                int noOfForgottenPasswordsDeleted = forgottenPasswordDao.deleteForgottenPasswords(deleteUsersAndRelatedDataBeforeDate);
                int noOfUsersDeleted = userDao.deleteUsersNotAssociatedWithAnyService(deleteUsersAndRelatedDataBeforeDate.toInstant());

                int noOfServicesArchived = archiveServices();

                LOGGER.info("Completed expunging and archiving historical data",
                        kv("no_of_users_deleted", noOfUsersDeleted),
                        kv("no_of_forgotten_passwords_deleted", noOfForgottenPasswordsDeleted),
                        kv("no_of_invites_deleted", noOfInvitesDeleted),
                        kv("no_of_services_archived", noOfServicesArchived)
                );
            } else {
                LOGGER.info("Expunging and archiving historical data is not enabled");
            }
        } finally {
            responseTimeTimer.observeDuration();
        }
    }

    private int archiveServices() {
        ZonedDateTime archiveServicesBeforeDate = getArchiveServicesBeforeDate();
        List<ServiceEntity> servicesToCheckForArchiving = serviceDao.findServicesToCheckForArchiving(archiveServicesBeforeDate);

        AtomicInteger numberOfServicesArchived = new AtomicInteger();
        servicesToCheckForArchiving.forEach(serviceEntity -> {

            ZonedDateTime lastTransactionDateForService = getLastTransactionDateForService(serviceEntity);

            if (canArchiveService(serviceEntity, lastTransactionDateForService)) {
                numberOfServicesArchived.getAndIncrement();
                serviceEntity.setArchived(true);
                serviceEntity.setArchivedDate(clock.instant().atZone(UTC));

                serviceDao.merge(serviceEntity);
                detachUsers(serviceEntity);

                connectorTaskQueue.addTaskToQueue(
                        new ConnectorTask(new ServiceArchivedTaskData(serviceEntity.getExternalId()), "service_archived"));

                LOGGER.info("Archived service", kv(SERVICE_EXTERNAL_ID, serviceEntity.getExternalId()));
            } else {
                if (serviceEntity.getFirstCheckedForArchivalDate() == null) {
                    serviceEntity.setFirstCheckedForArchivalDate(clock.instant().atZone(UTC));
                }

                ZonedDateTime skipCheckingUntilDate = calculateSkipCheckingForArchivalUntilDate(serviceEntity, lastTransactionDateForService);
                serviceEntity.setSkipCheckingForArchivalUntilDate(skipCheckingUntilDate);
            }
        });

        return numberOfServicesArchived.get();
    }

    private void detachUsers(ServiceEntity serviceEntity) {
        serviceRoleDao.removeUsersFromService(serviceEntity.getId());
    }

    private boolean canArchiveService(ServiceEntity serviceEntity, ZonedDateTime lastTransactionDateForService) {
        ZonedDateTime archiveServicesBeforeDate = getArchiveServicesBeforeDate();

        if (lastTransactionDateForService != null) {
            return lastTransactionDateForService.isBefore(archiveServicesBeforeDate);
        } else if (serviceEntity.getCreatedDate() != null) {
            return serviceEntity.getCreatedDate().isBefore(archiveServicesBeforeDate);
        } else if (serviceEntity.getFirstCheckedForArchivalDate() != null) {
            return serviceEntity.getFirstCheckedForArchivalDate().isBefore(archiveServicesBeforeDate);
        }

        return false;
    }

    private ZonedDateTime calculateSkipCheckingForArchivalUntilDate(ServiceEntity serviceEntity, ZonedDateTime lastTransactionDateForService) {
        ZonedDateTime skipCheckingUntilDate = null;
        if (lastTransactionDateForService != null) {
            skipCheckingUntilDate = lastTransactionDateForService.plusDays(expungeAndArchiveDataConfig.getArchiveServicesAfterDays());
        } else if (serviceEntity.getCreatedDate() != null) {
            skipCheckingUntilDate = serviceEntity.getCreatedDate().plusDays(expungeAndArchiveDataConfig.getArchiveServicesAfterDays());
        } else if (serviceEntity.getFirstCheckedForArchivalDate() != null) {
            skipCheckingUntilDate = serviceEntity.getFirstCheckedForArchivalDate().plusDays(expungeAndArchiveDataConfig.getArchiveServicesAfterDays());
        }
        return skipCheckingUntilDate;
    }

    private ZonedDateTime getLastTransactionDateForService(ServiceEntity serviceEntity) {
        return serviceEntity.getGatewayAccountIds()
                .stream()
                .map(gatewayAccountIdEntity -> getLastTransactionDateForGatewayAccount(gatewayAccountIdEntity.getGatewayAccountId()))
                .filter(Objects::nonNull)
                .max(Comparator.comparing(ZonedDateTime::toEpochSecond))
                .orElse(null);
    }

    private ZonedDateTime getLastTransactionDateForGatewayAccount(String gatewayAccountId) {
        LedgerSearchTransactionsResponse searchTransactions = ledgerService.searchTransactions(gatewayAccountId, 1);

        if (searchTransactions != null && !searchTransactions.getTransactions().isEmpty()) {
            return searchTransactions.getTransactions().get(0).getCreatedDate();
        }

        return null;
    }

    private ZonedDateTime getArchiveServicesBeforeDate() {
        return clock.instant()
                .minus(expungeAndArchiveDataConfig.getArchiveServicesAfterDays(), DAYS)
                .atZone(UTC);
    }

    private ZonedDateTime getDeleteUsersAndRelatedDataBeforeDate() {
        return clock.instant()
                .minus(expungeAndArchiveDataConfig.getExpungeUserDataAfterDays(), DAYS)
                .atZone(UTC);
    }
}
