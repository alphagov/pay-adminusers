package uk.gov.pay.adminusers.persistence.dao;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertTrue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserDaoTest extends DaoTestBase {

    private UserDao userDao;
    private ServiceDao serviceDao;
    private RoleDao roleDao;

    @Before
    public void before() throws Exception {
        userDao = env.getInstance(UserDao.class);
        serviceDao = env.getInstance(ServiceDao.class);
        roleDao = env.getInstance(RoleDao.class);
    }

    @Test
    public void shouldCreateAUserSuccessfully() throws Exception {
        Role role = roleDbFixture(databaseHelper).insertRole();
        String gatewayAccountId = randomInt().toString();
        int serviceId = serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(gatewayAccountId).insertService();

        String username = valueOf(nextInt());

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setPassword("password-" + username);
        userEntity.setDisabled(false);
        userEntity.setEmail(username + "@example.com");
        userEntity.setOtpKey(randomInt().toString());
        userEntity.setTelephoneNumber("876284762");
        userEntity.setSessionVersion(0);
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("UTC"));
        userEntity.setCreatedAt(timeNow);
        userEntity.setUpdatedAt(timeNow);

        ServiceEntity serviceEntity = serviceDao.findByGatewayAccountId(gatewayAccountId).get();
        RoleEntity roleEntity = roleDao.findByRoleName(role.getName()).get();

        ServiceRoleEntity serviceRoleEntity = new ServiceRoleEntity(serviceEntity, roleEntity);
        serviceRoleEntity.setUser(userEntity);

        userEntity.setServiceRole(serviceRoleEntity);

        userDao.persist(userEntity);

        assertThat(userEntity.getId(), is(notNullValue()));
        List<Map<String, Object>> savedUserData = databaseHelper.findUser(userEntity.getId());
        assertThat(savedUserData.size(), is(1));
        assertThat((String) savedUserData.get(0).get("external_id"), not(isEmptyOrNullString()));
        assertThat(((String) savedUserData.get(0).get("external_id")).length(), equalTo(32));
        assertThat(savedUserData.get(0).get("username"), is(userEntity.getUsername()));
        assertThat(savedUserData.get(0).get("password"), is(userEntity.getPassword()));
        assertThat(savedUserData.get(0).get("email"), is(userEntity.getEmail()));
        assertThat(savedUserData.get(0).get("otp_key"), is(userEntity.getOtpKey()));
        assertThat(savedUserData.get(0).get("telephone_number"), is(userEntity.getTelephoneNumber()));
        assertThat(savedUserData.get(0).get("disabled"), is(Boolean.FALSE));
        assertThat(savedUserData.get(0).get("session_version"), is(0));
        assertThat(savedUserData.get(0).get("createdAt"), is(java.sql.Timestamp.from(timeNow.toInstant())));
        assertThat(savedUserData.get(0).get("updatedAt"), is(java.sql.Timestamp.from(timeNow.toInstant())));

        List<Map<String, Object>> serviceRolesForUser = databaseHelper.findServiceRoleForUser(userEntity.getId());
        assertThat(serviceRolesForUser.size(), is(1));
        assertThat(serviceRolesForUser.get(0).get("id"), is(role.getId()));
        assertThat(serviceRolesForUser.get(0).get("service_id"), is(serviceId));
        assertThat(serviceRolesForUser.get(0).get("name"), is(role.getName()));
        assertThat(serviceRolesForUser.get(0).get("description"), is(role.getDescription()));
    }

    @Test
    public void shouldFindUserBy_ExternalId() throws Exception {
        Role role = roleDbFixture(databaseHelper).insertRole();
        int serviceId = serviceDbFixture(databaseHelper)
                .insertService();
        User user = userDbFixture(databaseHelper)
                .withServiceRole(serviceId, role.getId()).insertUser();

        String username = user.getUsername();
        String externalId = user.getExternalId();
        String otpKey = user.getOtpKey();

        Optional<UserEntity> userEntityMaybe = userDao.findByExternalId(externalId);
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getExternalId(), is(externalId));
        assertThat(foundUser.getUsername(), is(username));
        assertThat(foundUser.getEmail(), is(username + "@example.com"));
        assertThat(foundUser.getOtpKey(), is(otpKey));
        assertThat(foundUser.getTelephoneNumber(), is("374628482"));
        assertThat(foundUser.isDisabled(), is(false));
        assertThat(foundUser.getLoginCounter(), is(0));
        assertThat(foundUser.getSessionVersion(), is(0));
        assertThat(foundUser.getRoles().size(), is(1));
        assertThat(foundUser.getRoles().get(0).getId(), is(role.getId()));
    }

    @Test
    public void shouldFindUserBy_Username() throws Exception {
        Role role = roleDbFixture(databaseHelper).insertRole();
        int serviceId = serviceDbFixture(databaseHelper)
                .insertService();
        User user = userDbFixture(databaseHelper)
                .withServiceRole(serviceId, role.getId()).insertUser();

        String username = user.getUsername();
        String otpKey = user.getOtpKey();

        Optional<UserEntity> userEntityMaybe = userDao.findByUsername(username);
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getEmail(), is(username + "@example.com"));
        assertThat(foundUser.getUsername(), is(username));
        assertThat(foundUser.getOtpKey(), is(otpKey));
        assertThat(foundUser.getTelephoneNumber(), is("374628482"));
        assertThat(foundUser.isDisabled(), is(false));
        assertThat(foundUser.getLoginCounter(), is(0));
        assertThat(foundUser.getSessionVersion(), is(0));
        assertThat(foundUser.getRoles().size(), is(1));
        assertThat(foundUser.getRoles().get(0).getId(), is(role.getId()));
    }

    @Test
    public void shouldFindUser_ByEmail() throws Exception {
        Role role = roleDbFixture(databaseHelper).insertRole();
        int serviceId = serviceDbFixture(databaseHelper).insertService();
        User user = userDbFixture(databaseHelper)
                .withServiceRole(serviceId, role.getId()).insertUser();

        String username = user.getUsername();
        String otpKey = user.getOtpKey();
        String email = user.getEmail();

        Optional<UserEntity> userEntityMaybe = userDao.findByEmail(username + "@example.com");
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getUsername(), is(username));
        assertThat(foundUser.getEmail(), is(email));
        assertThat(foundUser.getOtpKey(), is(otpKey));
        assertThat(foundUser.getTelephoneNumber(), is("374628482"));
        assertThat(foundUser.isDisabled(), is(false));
        assertThat(foundUser.getLoginCounter(), is(0));
        assertThat(foundUser.getSessionVersion(), is(0));
        assertThat(foundUser.getRoles().size(), is(1));
        assertThat(foundUser.getRoles().get(0).getId(), is(role.getId()));
    }

    @Test
    public void shouldOverrideServiceRoleOfAnExistingUser_whenSettingANewServiceRole() {
        Role role1 = roleDbFixture(databaseHelper).insertRole();
        Role role2 = roleDbFixture(databaseHelper).insertRole();

        String gatewayAccountId1 = randomInt().toString();
        String gatewayAccountId2 = randomInt().toString();

        int serviceId1 = serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(gatewayAccountId1).insertService();
        serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(gatewayAccountId2).insertService();

        User user = userDbFixture(databaseHelper)
                .withServiceRole(serviceId1, role1.getId()).insertUser();

        String username = user.getUsername();
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

    @Test
    public void shouldFindUsers_ByServiceId_OrderedByUsername() {

        Role role1 = roleDbFixture(databaseHelper)
                        .withName("view")
                        .insertRole();

        Role role2 = roleDbFixture(databaseHelper)
                        .withName("admin")
                        .insertRole();

        int serviceId = serviceDbFixture(databaseHelper).insertService();

        User user1 = userDbFixture(databaseHelper)
                .withUsername("thomas")
                .withServiceRole(serviceId, role1.getId()).insertUser();

        User user2 = userDbFixture(databaseHelper)
                .withUsername("bob")
                .withServiceRole(serviceId, role2.getId()).insertUser();

        List<UserEntity> users = userDao.findByServiceId(serviceId);

        assertThat(users.size(), is(2));
        assertThat(users.get(0).getId(), is(user2.getId()));
        assertThat(users.get(1).getId(), is(user1.getId()));
    }

    @Test
    public void shouldNotFindAnyUser() {
        int serviceId = serviceDbFixture(databaseHelper).insertService();

        List<UserEntity> users = userDao.findByServiceId(serviceId);

        assertThat(users.isEmpty(), is(true));
    }
}
