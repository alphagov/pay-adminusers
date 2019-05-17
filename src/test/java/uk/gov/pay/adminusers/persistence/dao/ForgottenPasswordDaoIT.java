package uk.gov.pay.adminusers.persistence.dao;

import org.junit.Before;
import org.junit.Test;
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

import static java.time.temporal.ChronoUnit.MINUTES;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;
import static uk.gov.pay.adminusers.model.ForgottenPassword.forgottenPassword;

public class ForgottenPasswordDaoIT extends DaoTestBase {

    private UserDao userDao;
    private ForgottenPasswordDao forgottenPasswordDao;

    @Before
    public void before() {
        userDao = env.getInstance(UserDao.class);
        forgottenPasswordDao = env.getInstance(ForgottenPasswordDao.class);
    }

    @Test
    public void shouldPersistAForgottenPasswordEntity() {
        String forgottenPasswordCode = random(10);
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withUsername(username).withEmail(email).insertUser();
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
    public void shouldFindForgottenPasswordByCode_ifNotExpired() {
        String forgottenPasswordCode = random(10);
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withUsername(username).withEmail(email).insertUser();
        String userExternalId = user.getExternalId();
        UserEntity userEntity = userDao.findByExternalId(userExternalId).get();

        ZonedDateTime notExpired = ZonedDateTime.now().minusMinutes(89);
        ForgottenPassword forgottenPassword = forgottenPassword(randomInt(), forgottenPasswordCode, notExpired, userExternalId);

        databaseHelper.add(forgottenPassword, userEntity.getId());

        Optional<ForgottenPasswordEntity> forgottenPasswordEntityOptional = forgottenPasswordDao.findNonExpiredByCode(forgottenPassword.getCode());
        assertTrue(forgottenPasswordEntityOptional.isPresent());

        ForgottenPasswordEntity forgottenPasswordEntity = forgottenPasswordEntityOptional.get();
        assertThat(forgottenPasswordEntity.getCode(), is(forgottenPassword.getCode()));
        assertThat(forgottenPasswordEntity.getDate(), within(1, MINUTES, forgottenPassword.getDate()));
    }

    @Test
    public void shouldNotFindForgottenPasswordByCode_ifExpired() {
        String forgottenPasswordCode = randomUuid();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withUsername(username).withEmail(email).insertUser();
        String userExternalId = user.getExternalId();
        UserEntity userEntity = userDao.findByExternalId(userExternalId).get();

        ZonedDateTime expired = ZonedDateTime.now().minusMinutes(91);
        ForgottenPassword forgottenPassword = forgottenPassword(randomInt(), forgottenPasswordCode, expired, userExternalId);

        databaseHelper.add(forgottenPassword, userEntity.getId());

        Optional<ForgottenPasswordEntity> forgottenPasswordEntityOptional = forgottenPasswordDao.findNonExpiredByCode(forgottenPassword.getCode());
        assertFalse(forgottenPasswordEntityOptional.isPresent());
    }

    @Test
    public void shouldRemoveForgottenPasswordEntity() {
        String forgottenPasswordCode = randomUuid();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper).withUsername(username).withEmail(email).insertUser();
        String userExternalId = user.getExternalId();
        UserEntity userEntity = userDao.findByExternalId(userExternalId).get();

        ZonedDateTime notExpired = ZonedDateTime.now().minusMinutes(89);
        ForgottenPassword forgottenPassword = forgottenPassword(randomInt(), forgottenPasswordCode, notExpired, userExternalId);

        databaseHelper.add(forgottenPassword, userEntity.getId());

        Optional<ForgottenPasswordEntity> forgottenPasswordEntityOptional = forgottenPasswordDao.findNonExpiredByCode(forgottenPassword.getCode());
        assertTrue(forgottenPasswordEntityOptional.isPresent());

        forgottenPasswordDao.remove(forgottenPasswordEntityOptional.get());

        assertThat(forgottenPasswordDao.findNonExpiredByCode(forgottenPassword.getCode()).isPresent(), is(false));
    }
}
