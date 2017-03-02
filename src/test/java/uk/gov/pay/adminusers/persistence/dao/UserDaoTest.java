package uk.gov.pay.adminusers.persistence.dao;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.*;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertTrue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

public class UserDaoTest extends DaoTestBase {

    private UserDao userDao;
    private ServiceDao serviceDao;
    private RoleDao roleDao;
    private String random;
    private Integer randomInt;

    @Before
    public void before() throws Exception {
        userDao = env.getInstance(UserDao.class);
        serviceDao = env.getInstance(ServiceDao.class);
        roleDao = env.getInstance(RoleDao.class);
        random = newId();
        randomInt = randomInt();
    }

    @Test
    public void shouldCreateAUserSuccessfully() throws Exception {
        Permission perm1 = aPermission();
        Permission perm2 = aPermission();
        databaseTestHelper.add(perm1).add(perm2);

        Role role1 = aRole();
        role1.setPermissions(asList(perm1, perm2));
        databaseTestHelper.add(role1);

        String gatewayAccountId = randomInt.toString();
        int serviceId = nextInt();
        databaseTestHelper.addService(serviceId, gatewayAccountId);

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername("user-" + random);
        userEntity.setPassword("password-" + random);
        userEntity.setDisabled(false);
        userEntity.setEmail(random + "@example.com");
        userEntity.setOtpKey(randomInt.toString());
        userEntity.setTelephoneNumber("876284762");
        userEntity.setSessionVersion(0);
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("UTC"));
        userEntity.setCreatedAt(timeNow);
        userEntity.setUpdatedAt(timeNow);

        ServiceEntity serviceEntity1 = serviceDao.findByGatewayAccountId(gatewayAccountId).get();
        RoleEntity roleEntity1 = roleDao.findByRoleName(role1.getName()).get();

        ServiceRoleEntity serviceRoleEntity = new ServiceRoleEntity(serviceEntity1, roleEntity1);
        serviceRoleEntity.setUser(userEntity);

        userEntity.setServiceRole(serviceRoleEntity);

        userDao.persist(userEntity);

        assertThat(userEntity.getId(), is(notNullValue()));
        List<Map<String, Object>> savedUserData = databaseTestHelper.findUser(userEntity.getId());
        assertThat(savedUserData.size(), is(1));
        assertThat(savedUserData.get(0).get("username"), is(userEntity.getUsername()));
        assertThat(savedUserData.get(0).get("password"), is(userEntity.getPassword()));
        assertThat(savedUserData.get(0).get("email"), is(userEntity.getEmail()));
        assertThat(savedUserData.get(0).get("otp_key"), is(userEntity.getOtpKey()));
        assertThat(savedUserData.get(0).get("telephone_number"), is(userEntity.getTelephoneNumber()));
        assertThat(savedUserData.get(0).get("disabled"), is(Boolean.FALSE));
        assertThat(savedUserData.get(0).get("session_version"), is(0));
        assertThat(savedUserData.get(0).get("createdAt"), is(java.sql.Timestamp.from(timeNow.toInstant())));
        assertThat(savedUserData.get(0).get("updatedAt"), is(java.sql.Timestamp.from(timeNow.toInstant())));

        List<Map<String, Object>> serviceRolesForUser = databaseTestHelper.findServiceRoleForUser(userEntity.getId());
        assertThat(serviceRolesForUser.size(), is(1));
        assertThat(serviceRolesForUser.get(0).get("id"), is(role1.getId()));
        assertThat(serviceRolesForUser.get(0).get("service_id"), is(serviceId));
        assertThat(serviceRolesForUser.get(0).get("name"), is(role1.getName()));
        assertThat(serviceRolesForUser.get(0).get("description"), is(role1.getDescription()));
    }

    @Test
    public void shouldFindUserBy_Username() throws Exception {
        Permission perm1 = aPermission();
        Permission perm2 = aPermission();
        Permission perm3 = aPermission();
        databaseTestHelper.add(perm1).add(perm2).add(perm3);

        Role role = aRole();
        role.setPermissions(asList(perm1, perm2, perm3));
        RoleEntity roleEntity = new RoleEntity(role);
        databaseTestHelper.add(role);

        String gatewayAccountId = randomInt.toString();
        int serviceId = nextInt();
        databaseTestHelper.addService(serviceId, gatewayAccountId);

        String username = "user-" + random;
        User user = User.from(randomInt(), username, "password-" + random, random + "@example.com", asList(gatewayAccountId), randomInt.toString(), "374628482");
        databaseTestHelper.add(user, serviceId, role.getId());

        Optional<UserEntity> userEntityMaybe = userDao.findByUsername(username);
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getEmail(), is(random + "@example.com"));
        assertThat(foundUser.getUsername(), is(username));
        assertThat(foundUser.getOtpKey(), is(randomInt.toString()));
        assertThat(foundUser.getTelephoneNumber(), is("374628482"));
        assertThat(foundUser.isDisabled(), is(false));
        assertThat(foundUser.getLoginCounter(), is(0));
        assertThat(foundUser.getSessionVersion(), is(0));
        assertThat(foundUser.getRoles().size(), is(1));
        assertThat(foundUser.getRoles().get(0).getId(), is(roleEntity.getId()));
    }

    @Test
    public void shouldFindUser_ByEmail() throws Exception {
        Permission perm1 = aPermission();
        Permission perm2 = aPermission();
        Permission perm3 = aPermission();
        databaseTestHelper.add(perm1).add(perm2).add(perm3);

        Role role = aRole();
        role.setPermissions(asList(perm1, perm2, perm3));
        RoleEntity roleEntity = new RoleEntity(role);
        databaseTestHelper.add(role);

        String gatewayAccountId = randomInt.toString();
        int serviceId = nextInt();
        databaseTestHelper.addService(serviceId, gatewayAccountId);

        String email = random + "@example.com";
        User user = User.from(randomInt(), "user-" + random, "password-" + random, email, asList(randomInt.toString()), randomInt.toString(), "374628482");
        databaseTestHelper.add(user, serviceId, role.getId());

        Optional<UserEntity> userEntityMaybe = userDao.findByEmail(email);
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getUsername(), is("user-" + random));
        assertThat(foundUser.getEmail(), is(email));
        assertThat(foundUser.getOtpKey(), is(randomInt.toString()));
        assertThat(foundUser.getTelephoneNumber(), is("374628482"));
        assertThat(foundUser.isDisabled(), is(false));
        assertThat(foundUser.getLoginCounter(), is(0));
        assertThat(foundUser.getSessionVersion(), is(0));
        assertThat(foundUser.getRoles().size(), is(1));
        assertThat(foundUser.getRoles().get(0).getId(), is(roleEntity.getId()));
    }

    @Test
    public void shouldOverrideServiceRoleOfAnExistingUser_whenSettingANewServiceRole() {
        Permission perm = aPermission();
        databaseTestHelper.add(perm);

        Role role1 = aRole();
        Role role2 = aRole();
        role1.setPermissions(asList(perm));
        role2.setPermissions(asList(perm));
        databaseTestHelper.add(role1);
        databaseTestHelper.add(role2);

        String gatewayAccountId1 = valueOf(nextInt());
        String gatewayAccountId2 = valueOf(nextInt());
        int serviceId1 = nextInt();
        int serviceId2 = nextInt();

        databaseTestHelper.addService(serviceId1, gatewayAccountId1);
        databaseTestHelper.addService(serviceId2, gatewayAccountId2);

        String username = "user-" + random;
        databaseTestHelper.add(User.from(randomInt(), username, "password", random + "@example.com", asList(gatewayAccountId1), randomInt.toString(), "876284762"), serviceId1, role1.getId());

        UserEntity existingUser = userDao.findByUsername(username).get();

        assertThat(existingUser.getGatewayAccountId(), is(gatewayAccountId1));
        assertThat(existingUser.getRoles().size(), is(1));
        assertThat(existingUser.getRoles().get(0).getId(), is(role1.getId()));

        ServiceEntity serviceEntity2 = serviceDao.findByGatewayAccountId(gatewayAccountId2).get();
        RoleEntity roleEntity2 = roleDao.findByRoleName(role2.getName()).get();

        ServiceRoleEntity serviceRole = new ServiceRoleEntity(serviceEntity2, roleEntity2);
        serviceRole.setUser(existingUser);
        existingUser.setServiceRole(serviceRole);
        userDao.persist(existingUser);

        UserEntity changedUser = userDao.findByUsername(username).get();
        assertThat(changedUser.getGatewayAccountId(), is(gatewayAccountId2));
        assertThat(changedUser.getRoles().size(), is(1));
        assertThat(changedUser.getRoles().get(0).getId(), is(role2.getId()));
    }
}
