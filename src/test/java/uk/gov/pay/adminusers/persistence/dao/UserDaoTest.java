package uk.gov.pay.adminusers.persistence.dao;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertTrue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newLongId;

public class UserDaoTest extends DaoTestBase {

    private UserDao userDao;
    String random;
    Long randomLong;

    @Before
    public void before() throws Exception {
        userDao = env.getInstance(UserDao.class);
        random = newId();
        randomLong = newLongId();
    }

    @Test
    public void shouldCreateAUserSuccessfully() throws Exception {
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername("user-" + random);
        userEntity.setPassword("password-" + random);
        userEntity.setDisabled(false);
        userEntity.setEmail(random + "@example.com");
        userEntity.setGatewayAccountId(randomLong.toString());
        userEntity.setOtpKey(randomLong.toString());
        userEntity.setTelephoneNumber("876284762");

        userDao.persist(userEntity);
        assertThat(userEntity.getId(), is(notNullValue()));
        List<Map<String, Object>> savedUserData = databaseTestHelper.findUser(userEntity.getId());
        assertThat(savedUserData.size(), is(1));
        assertThat(savedUserData.get(0).get("username"), is(userEntity.getUsername()));
        assertThat(savedUserData.get(0).get("password"), is(userEntity.getPassword()));
        assertThat(savedUserData.get(0).get("email"), is(userEntity.getEmail()));
        assertThat(savedUserData.get(0).get("otp_key"), is(userEntity.getOtpKey()));
        assertThat(savedUserData.get(0).get("telephone_number"), is(userEntity.getTelephoneNumber()));
        assertThat(savedUserData.get(0).get("gateway_account_id"), is(userEntity.getGatewayAccountId()));
        assertThat(savedUserData.get(0).get("disabled"), is(Boolean.FALSE));
    }

    @Test
    public void shouldFindUserByUsername() throws Exception {
        String username = "user-" + random;
        User user = User.from(username, "password-" + random, random + "@example.com", randomLong.toString(), randomLong.toString(), "374628482");
        databaseTestHelper.addUser(user);

        Optional<UserEntity> userEntityMaybe = userDao.findByUsername(username);
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getEmail(), is(random + "@example.com"));
        assertThat(foundUser.getUsername(), is(username));
        assertThat(foundUser.getGatewayAccountId(), is(randomLong.toString()));
        assertThat(foundUser.getOtpKey(), is(randomLong.toString()));
        assertThat(foundUser.getTelephoneNumber(), is("374628482"));
        assertThat(foundUser.getDisabled(), is(false));
        assertThat(foundUser.getLoginCount(), is(0));
    }

    @Test
    public void shouldFindUserByEmail() throws Exception {
        String email = random + "@example.com";
        User user = User.from("user-" + random, "password-" + random, email, randomLong.toString(), randomLong.toString(), "374628482");
        databaseTestHelper.addUser(user);

        Optional<UserEntity> userEntityMaybe = userDao.findByEmail(email);
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getUsername(), is("user-" + random));
        assertThat(foundUser.getEmail(), is(email));
        assertThat(foundUser.getGatewayAccountId(), is(randomLong.toString()));
        assertThat(foundUser.getOtpKey(), is(randomLong.toString()));
        assertThat(foundUser.getTelephoneNumber(), is("374628482"));
        assertThat(foundUser.getDisabled(), is(false));
        assertThat(foundUser.getLoginCount(), is(0));
    }
}
