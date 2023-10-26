package uk.gov.pay.adminusers.persistence.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.fixtures.ForgottenPasswordDbFixture;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.ZonedDateTime.parse;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.ForgottenPasswordDbFixture.aForgottenPasswordDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;
import static uk.gov.pay.adminusers.model.ForgottenPassword.forgottenPassword;

class ForgottenPasswordDaoIT extends DaoTestBase {

    private UserDao userDao;
    private ForgottenPasswordDao forgottenPasswordDao;

    @BeforeEach
    public void before() {
        userDao = env.getInstance(UserDao.class);
        forgottenPasswordDao = env.getInstance(ForgottenPasswordDao.class);
    }

    @Test
    void shouldPersistAForgottenPasswordEntity() {
        String forgottenPasswordCode = random(10);
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withEmail(email).insertUser();
        String userExternalId = user.getExternalId();
        UserEntity userEntity = userDao.findByExternalId(userExternalId).get();

        ForgottenPassword forgottenPassword = forgottenPassword(forgottenPasswordCode, userExternalId);
        ForgottenPasswordEntity forgottenPasswordEntity = ForgottenPasswordEntity.from(forgottenPassword, userEntity);

        forgottenPasswordDao.persist(forgottenPasswordEntity);

        assertThat(forgottenPasswordEntity.getId(), is(notNullValue()));
        List<Map<String, Object>> forgottenPasswordById = databaseHelper.findForgottenPasswordById(forgottenPasswordEntity.getId());

        assertThat(forgottenPasswordById.size(), is(1));
        assertThat(forgottenPasswordById.get(0).get("code"), is(forgottenPasswordCode));

        Timestamp storedDate = (Timestamp) forgottenPasswordById.get(0).get("date");
        ZonedDateTime storedDateTime = ZonedDateTime.ofInstant(storedDate.toInstant(), ZoneId.of("UTC"));
        assertThat(storedDateTime, within(1, MINUTES, forgottenPassword.getDate()));
    }

    @Test
    void shouldFindForgottenPasswordByCode_ifNotExpired() {
        String forgottenPasswordCode = random(10);
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withEmail(email).insertUser();
        String userExternalId = user.getExternalId();
        UserEntity userEntity = userDao.findByExternalId(userExternalId).get();

        ZonedDateTime notExpired = ZonedDateTime.now().minusMinutes(89);

        aForgottenPasswordDbFixture()
                .withDatabaseTestHelper(databaseHelper)
                .withUserId(userEntity.getId())
                .withExpiryDate(notExpired)
                .withCode(forgottenPasswordCode)
                .insert();

        Optional<ForgottenPasswordEntity> forgottenPasswordEntityOptional = forgottenPasswordDao.findNonExpiredByCode(forgottenPasswordCode);
        assertTrue(forgottenPasswordEntityOptional.isPresent());

        ForgottenPasswordEntity forgottenPasswordEntity = forgottenPasswordEntityOptional.get();
        assertThat(forgottenPasswordEntity.getCode(), is(forgottenPasswordCode));
        assertThat(forgottenPasswordEntity.getDate(), within(1, MINUTES, notExpired));
    }

    @Test
    void shouldNotFindForgottenPasswordByCode_ifExpired() {
        String forgottenPasswordCode = randomUuid();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withEmail(email).insertUser();
        String userExternalId = user.getExternalId();
        UserEntity userEntity = userDao.findByExternalId(userExternalId).get();

        ZonedDateTime expired = ZonedDateTime.now().minusMinutes(91);

        aForgottenPasswordDbFixture()
                .withDatabaseTestHelper(databaseHelper)
                .withUserId(userEntity.getId())
                .withExpiryDate(expired)
                .withCode(forgottenPasswordCode)
                .insert();

        Optional<ForgottenPasswordEntity> forgottenPasswordEntityOptional = forgottenPasswordDao.findNonExpiredByCode(forgottenPasswordCode);
        assertFalse(forgottenPasswordEntityOptional.isPresent());
    }

    @Test
    void shouldRemoveForgottenPasswordEntity() {
        String forgottenPasswordCode = randomUuid();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withEmail(email).insertUser();
        String userExternalId = user.getExternalId();
        UserEntity userEntity = userDao.findByExternalId(userExternalId).get();

        ZonedDateTime notExpired = ZonedDateTime.now().minusMinutes(89);

        aForgottenPasswordDbFixture()
                .withDatabaseTestHelper(databaseHelper)
                .withUserId(userEntity.getId())
                .withExpiryDate(notExpired)
                .withCode(forgottenPasswordCode)
                .insert();

        Optional<ForgottenPasswordEntity> forgottenPasswordEntityOptional = forgottenPasswordDao.findNonExpiredByCode(forgottenPasswordCode);
        assertTrue(forgottenPasswordEntityOptional.isPresent());

        forgottenPasswordDao.remove(forgottenPasswordEntityOptional.get());

        assertThat(forgottenPasswordDao.findNonExpiredByCode(forgottenPasswordCode).isPresent(), is(false));
    }

    @Nested
    class TestDeleteForgottenPasswords {

        @Test
        void shouldRemoveForgottenPasswordsOlderThanTheDateProvided() {
            User user = userDbFixture(databaseHelper).insertUser();
            ZonedDateTime deleteRecordsUpToDate = parse("2020-01-01T00:00:00Z");

            ForgottenPasswordDbFixture forgottenPasswordDbFixture = aForgottenPasswordDbFixture()
                    .withDatabaseTestHelper(databaseHelper)
                    .withUserId(user.getId())
                    .withCreatedAt(deleteRecordsUpToDate.minusDays(1))
                    .insert();
            ForgottenPasswordDbFixture forgottenPasswordDbFixture2 = aForgottenPasswordDbFixture()
                    .withDatabaseTestHelper(databaseHelper)
                    .withUserId(user.getId())
                    .withCreatedAt(deleteRecordsUpToDate.minusDays(2))
                    .insert();

            int noOfRecordsDeleted = forgottenPasswordDao.deleteForgottenPasswords(deleteRecordsUpToDate);

            assertThat(noOfRecordsDeleted, is(2));

            Optional<ForgottenPasswordEntity> forgottenPasswordEntityOptional = forgottenPasswordDao.findById(forgottenPasswordDbFixture.getId());
            assertFalse(forgottenPasswordEntityOptional.isPresent());

            forgottenPasswordEntityOptional = forgottenPasswordDao.findById(forgottenPasswordDbFixture2.getId());
            assertFalse(forgottenPasswordEntityOptional.isPresent());
        }

        @Test
        void shouldNotRemoveForgottenPasswordsCreatedAfterTheDateProvided() {
            User user = userDbFixture(databaseHelper).insertUser();
            ZonedDateTime deleteRecordsUpToDate = parse("2020-01-01T00:00:00Z");

            ForgottenPasswordDbFixture forgottenPasswordDbFixture = aForgottenPasswordDbFixture()
                    .withDatabaseTestHelper(databaseHelper)
                    .withUserId(user.getId())
                    .withCreatedAt(deleteRecordsUpToDate)
                    .insert();
            ForgottenPasswordDbFixture forgottenPasswordDbFixture2 = aForgottenPasswordDbFixture()
                    .withDatabaseTestHelper(databaseHelper)
                    .withUserId(user.getId())
                    .withCreatedAt(deleteRecordsUpToDate.plusDays(1))
                    .insert();

            int noOfRecordsDeleted = forgottenPasswordDao.deleteForgottenPasswords(deleteRecordsUpToDate);

            assertThat(noOfRecordsDeleted, is(0));

            Optional<ForgottenPasswordEntity> forgottenPasswordEntityOptional = forgottenPasswordDao.findById(forgottenPasswordDbFixture.getId());
            assertTrue(forgottenPasswordEntityOptional.isPresent());

            forgottenPasswordEntityOptional = forgottenPasswordDao.findById(forgottenPasswordDbFixture2.getId());
            assertTrue(forgottenPasswordEntityOptional.isPresent());
        }
    }
}
