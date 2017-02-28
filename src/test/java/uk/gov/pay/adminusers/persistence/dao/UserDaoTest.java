package uk.gov.pay.adminusers.persistence.dao;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertTrue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

public class UserDaoTest extends DaoTestBase {

    private UserDao userDao;
    private ServiceDao serviceDao;
    private String random;
    private Integer randomInt;

    @Before
    public void before() throws Exception {
        userDao = env.getInstance(UserDao.class);
        serviceDao = env.getInstance(ServiceDao.class);
        random = newId();
        randomInt = randomInt();
    }

    @Test
    public void shouldCreateAUserSuccessfully() throws Exception {

        Permission perm1 = aPermission();
        Permission perm2 = aPermission();
        Permission perm3 = aPermission();
        Permission perm4 = aPermission();
        Role role1 = aRole();
        Role role2 = aRole();
        role1.setPermissions(asList(perm1, perm2));
        role2.setPermissions(asList(perm3, perm4));

        String gatewayAccountId = randomInt.toString();
        databaseTestHelper.addService(RandomUtils.nextInt(), gatewayAccountId);
        databaseTestHelper.add(perm1).add(perm2).add(perm3).add(perm4);
        databaseTestHelper.add(role1).add(role2);

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername("user-" + random);
        userEntity.setPassword("password-" + random);
        userEntity.setDisabled(false);
        userEntity.setEmail(random + "@example.com");
        userEntity.setGatewayAccountId(gatewayAccountId);
        userEntity.addService(serviceDao.findByGatewayAccountId(gatewayAccountId).get());
        userEntity.setOtpKey(randomInt.toString());
        userEntity.setTelephoneNumber("876284762");
        userEntity.setRoles(asList(new RoleEntity(role1), new RoleEntity(role2)));
        userEntity.setSessionVersion(0);
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("UTC"));
        userEntity.setCreatedAt(timeNow);
        userEntity.setUpdatedAt(timeNow);

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
        assertThat(savedUserData.get(0).get("session_version"), is(0));
        assertThat(savedUserData.get(0).get("createdAt"), is(java.sql.Timestamp.from(timeNow.toInstant())));
        assertThat(savedUserData.get(0).get("updatedAt"), is(java.sql.Timestamp.from(timeNow.toInstant())));

        List<Map<String, Object>> rolesForUser = databaseTestHelper.findRolesForUser(userEntity.getId());
        assertThat(rolesForUser.size(), is(2));
        assertThat((Integer) rolesForUser.get(0).get("id"), either(is(role1.getId())).or(is(role2.getId())));
        assertThat((String) rolesForUser.get(0).get("name"), either(is(role1.getName())).or(is(role2.getName())));
        assertThat((String) rolesForUser.get(0).get("description"), either(is(role1.getDescription())).or(is(role2.getDescription())));

        assertThat((Integer) rolesForUser.get(1).get("id"), either(is(role1.getId())).or(is(role2.getId())));
        assertThat((String) rolesForUser.get(1).get("name"), either(is(role1.getName())).or(is(role2.getName())));
        assertThat((String) rolesForUser.get(1).get("description"), either(is(role1.getDescription())).or(is(role2.getDescription())));

    }

    @Test
    public void shouldFindUserBy_Username() throws Exception {
        Permission perm1 = aPermission();
        Permission perm2 = aPermission();
        Permission perm3 = aPermission();
        Role role1 = aRole();
        Role role2 = aRole();
        role1.setPermissions(asList(perm1, perm2, perm3));
        role2.setPermissions(asList(perm2, perm3));
        String gatewayAccountId = randomInt.toString();
        int serviceId = RandomUtils.nextInt();

        databaseTestHelper.addService(serviceId, gatewayAccountId);
        databaseTestHelper.add(perm1).add(perm2).add(perm3);
        databaseTestHelper.add(role1).add(role2);

        String username = "user-" + random;
        User user = User.from(randomInt(), username, "password-" + random, random + "@example.com", gatewayAccountId, randomInt.toString(), "374628482");
        user.setRoles(asList(role1, role2));
        databaseTestHelper.add(user, serviceId);

        Optional<UserEntity> userEntityMaybe = userDao.findByUsername(username);
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getEmail(), is(random + "@example.com"));
        assertThat(foundUser.getUsername(), is(username));
        assertThat(foundUser.getGatewayAccountId(), is(gatewayAccountId));
        assertThat(foundUser.getOtpKey(), is(randomInt.toString()));
        assertThat(foundUser.getTelephoneNumber(), is("374628482"));
        assertThat(foundUser.isDisabled(), is(false));
        assertThat(foundUser.getLoginCounter(), is(0));
        assertThat(foundUser.getSessionVersion(), is(0));
        assertThat(foundUser.getRoles().size(), is(2));
        assertThat(foundUser.getRoles().get(0).toRole(), either(is(role1)).or(is(role2)));
        assertThat(foundUser.getRoles().get(1).toRole(), either(is(role1)).or(is(role2)));
    }

    @Test
    public void shouldFindUser_ByEmail() throws Exception {
        Permission perm1 = aPermission();
        Permission perm2 = aPermission();
        Permission perm3 = aPermission();
        Role role1 = aRole();
        Role role2 = aRole();
        role1.setPermissions(asList(perm1, perm2, perm3));
        role2.setPermissions(asList(perm2, perm3));

        String gatewayAccountId = randomInt.toString();
        int serviceId = RandomUtils.nextInt();

        databaseTestHelper.addService(serviceId, gatewayAccountId);
        databaseTestHelper.add(perm1).add(perm2).add(perm3);
        databaseTestHelper.add(role1).add(role2);

        String email = random + "@example.com";
        User user = User.from(randomInt(), "user-" + random, "password-" + random, email, randomInt.toString(), randomInt.toString(), "374628482");
        user.setRoles(asList(role1, role2));
        databaseTestHelper.add(user, serviceId);

        Optional<UserEntity> userEntityMaybe = userDao.findByEmail(email);
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getUsername(), is("user-" + random));
        assertThat(foundUser.getEmail(), is(email));
        assertThat(foundUser.getGatewayAccountId(), is(gatewayAccountId));
        assertThat(foundUser.getOtpKey(), is(randomInt.toString()));
        assertThat(foundUser.getTelephoneNumber(), is("374628482"));
        assertThat(foundUser.isDisabled(), is(false));
        assertThat(foundUser.getLoginCounter(), is(0));
        assertThat(foundUser.getSessionVersion(), is(0));
        assertThat(foundUser.getRoles().size(), is(2));
        assertThat(foundUser.getRoles().get(0).toRole(), either(is(role1)).or(is(role2)));
        assertThat(foundUser.getRoles().get(1).toRole(), either(is(role1)).or(is(role2)));
    }
}
