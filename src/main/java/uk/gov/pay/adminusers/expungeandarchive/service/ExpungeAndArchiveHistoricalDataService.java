package uk.gov.pay.adminusers.expungeandarchive.service;

import com.google.inject.Inject;
import io.prometheus.client.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.ExpungeAndArchiveDataConfig;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.InviteDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;

import java.time.Clock;
import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static net.logstash.logback.argument.StructuredArguments.kv;

public class ExpungeAndArchiveHistoricalDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpungeAndArchiveHistoricalDataService.class);
    private final UserDao userDao;
    private final InviteDao inviteDao;
    private final ForgottenPasswordDao forgottenPasswordDao;
    private final ExpungeAndArchiveDataConfig expungeAndArchiveDataConfig;
    private final Clock clock;

    private static final Histogram duration = Histogram.build()
            .name("expunge_and_archive_historical_data_job_duration_seconds")
            .help("Duration of expunge and archive historical data job in seconds")
            .unit("seconds")
            .register();

    @Inject
    public ExpungeAndArchiveHistoricalDataService(UserDao userDao, InviteDao inviteDao,
                                                  ForgottenPasswordDao forgottenPasswordDao,
                                                  AdminUsersConfig adminUsersConfig,
                                                  Clock clock) {
        this.userDao = userDao;
        this.inviteDao = inviteDao;
        this.forgottenPasswordDao = forgottenPasswordDao;
        expungeAndArchiveDataConfig = adminUsersConfig.getExpungeAndArchiveDataConfig();
        this.clock = clock;
    }

    public void expungeAndArchiveHistoricalData() {
        Histogram.Timer responseTimeTimer = duration.startTimer();

        try {
            if (expungeAndArchiveDataConfig.isExpungeAndArchiveHistoricalDataEnabled()) {
                ZonedDateTime deleteUsersAndRelatedDataBeforeDate = getDeleteUsersAndRelatedDataBeforeDate();

                int noOfUsersDeleted = userDao.deleteUsersNotAssociatedWithAnyService(deleteUsersAndRelatedDataBeforeDate.toInstant());
                int noOfInvitesDeleted = inviteDao.deleteInvites(deleteUsersAndRelatedDataBeforeDate);
                int noOfForgottenPasswordsDeleted = forgottenPasswordDao.deleteForgottenPasswords(deleteUsersAndRelatedDataBeforeDate);

                LOGGER.info("Completed expunging and archiving historical data",
                        kv("no_of_users_deleted", noOfUsersDeleted),
                        kv("no_of_forgotten_passwords_deleted", noOfForgottenPasswordsDeleted),
                        kv("no_of_invites_deleted", noOfInvitesDeleted));
            } else {
                LOGGER.info("Expunging and archiving historical data is not enabled");
            }
        } finally {
            responseTimeTimer.observeDuration();
        }
    }

    private ZonedDateTime getDeleteUsersAndRelatedDataBeforeDate() {
        ZonedDateTime expungeAndArchiveDateBeforeDate = clock.instant()
                .minus(expungeAndArchiveDataConfig.getExpungeUserDataAfterDays(), DAYS)
                .atZone(UTC);
        return expungeAndArchiveDateBeforeDate;
    }
}
