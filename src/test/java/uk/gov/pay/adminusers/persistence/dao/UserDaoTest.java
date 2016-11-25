package uk.gov.pay.adminusers.persistence.dao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newLongId;

public class UserDaoTest extends DaoTestBase {

    private UserDao userDao;

    @Before
    public void before() throws Exception {
        userDao = env.getInstance(UserDao.class);
    }

    @Test
    public void shouldCreateAUserSuccessfully() throws Exception {
        String random = newId();
        Long randomLong = newLongId();
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

    

}
