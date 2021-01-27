package uk.gov.pay.adminusers.persistence.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserDaoIT extends DaoTestBase {

    private UserDao userDao;
    private ServiceDao serviceDao;
    private RoleDao roleDao;

    @BeforeEach
    public void before() {
        userDao = env.getInstance(UserDao.class);
        serviceDao = env.getInstance(ServiceDao.class);
        roleDao = env.getInstance(RoleDao.class);
    }
    
    @Test
    void getAdminUserEmailsForGatewayAccountIds_should_return_empty_map() {
        Map<String, List<String>> map = userDao.getAdminUserEmailsForGatewayAccountIds(List.of());
        assertThat(map.isEmpty(), is(true));
    }

    @Test
    public void shouldCreateAUserSuccessfully() {
        Role role = roleDbFixture(databaseHelper).insertRole();
        String gatewayAccountId = randomInt().toString();
        int serviceId = serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(gatewayAccountId).insertService().getId();

        String username = valueOf(nextInt());

        UserEntity userEntity = new UserEntity();
        userEntity.setExternalId(randomUuid());
        userEntity.setUsername(username);
        userEntity.setPassword("password-" + username);
        userEntity.setDisabled(false);
        userEntity.setEmail(username + "@example.com");
        userEntity.setOtpKey(randomInt().toString());
        userEntity.setTelephoneNumber("+447700900000");
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setSessionVersion(0);
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("UTC"));
        userEntity.setCreatedAt(timeNow);
        userEntity.setUpdatedAt(timeNow);

        ServiceEntity serviceEntity = serviceDao.findByGatewayAccountId(gatewayAccountId).get();
        RoleEntity roleEntity = roleDao.findByRoleName(role.getName()).get();

        ServiceRoleEntity serviceRoleEntity = new ServiceRoleEntity(serviceEntity, roleEntity);
        serviceRoleEntity.setUser(userEntity);

        userEntity.addServiceRole(serviceRoleEntity);

        userDao.persist(userEntity);

        assertThat(userEntity.getId(), is(notNullValue()));
        List<Map<String, Object>> savedUserData = databaseHelper.findUser(userEntity.getId());
        assertThat(savedUserData.size(), is(1));
        assertThat((String) savedUserData.get(0).get("external_id"), not(emptyOrNullString()));
        assertThat(((String) savedUserData.get(0).get("external_id")).length(), equalTo(32));
        assertThat(savedUserData.get(0).get("username"), is(userEntity.getUsername()));
        assertThat(savedUserData.get(0).get("password"), is(userEntity.getPassword()));
        assertThat(savedUserData.get(0).get("email"), is(userEntity.getEmail()));
        assertThat(savedUserData.get(0).get("otp_key"), is(userEntity.getOtpKey()));
        assertThat(savedUserData.get(0).get("telephone_number"), is(userEntity.getTelephoneNumber()));
        assertThat(savedUserData.get(0).get("disabled"), is(Boolean.FALSE));
        assertThat(savedUserData.get(0).get("session_version"), is(0));
        assertThat(savedUserData.get(0).get("createdat"), is(java.sql.Timestamp.from(timeNow.toInstant())));
        assertThat(savedUserData.get(0).get("updatedat"), is(java.sql.Timestamp.from(timeNow.toInstant())));

        List<Map<String, Object>> serviceRolesForUser = databaseHelper.findServiceRoleForUser(userEntity.getId());
        assertThat(serviceRolesForUser.size(), is(1));
        assertThat(serviceRolesForUser.get(0).get("id"), is(role.getId()));
        assertThat(serviceRolesForUser.get(0).get("service_id"), is(serviceId));
        assertThat(serviceRolesForUser.get(0).get("name"), is(role.getName()));
        assertThat(serviceRolesForUser.get(0).get("description"), is(role.getDescription()));
    }

    @Test
    public void shouldFindUserBy_ExternalId() {
        Role role = roleDbFixture(databaseHelper).insertRole();
        int serviceId1 = serviceDbFixture(databaseHelper)
                .insertService().getId();
        int serviceId2 = serviceDbFixture(databaseHelper)
                .insertService().getId();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper)
                .withServiceRole(serviceId1, role.getId())
                .withServiceRole(serviceId2, role.getId())
                .withUsername(username)
                .withEmail(email)
                .insertUser();

        String externalId = user.getExternalId();
        String otpKey = user.getOtpKey();

        Optional<UserEntity> userEntityMaybe = userDao.findByExternalId(externalId);
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getExternalId(), is(externalId));
        assertThat(foundUser.getUsername(), is(username));
        assertThat(foundUser.getEmail(), is(email));
        assertThat(foundUser.getOtpKey(), is(otpKey));
        assertThat(foundUser.getTelephoneNumber(), is("+447700900000"));
        assertThat(foundUser.isDisabled(), is(false));
        assertThat(foundUser.getLoginCounter(), is(0));
        assertThat(foundUser.getSessionVersion(), is(0));
        assertThat(foundUser.getRoles().size(), is(1));
        assertThat(foundUser.toUser().getServiceRoles().size(), is(2));
        assertThat(foundUser.getRoles().get(0).getId(), is(role.getId()));
    }

    @Test
    public void shouldFindUsersBy_ExternalIds() {
        Role role = roleDbFixture(databaseHelper).insertRole();
        int serviceId1 = serviceDbFixture(databaseHelper)
                .insertService().getId();
        int serviceId2 = serviceDbFixture(databaseHelper)
                .insertService().getId();
        String username1 = randomUuid();
        String email1 = username1 + "@example.com";
        User user1 = userDbFixture(databaseHelper)
                .withServiceRole(serviceId1, role.getId())
                .withServiceRole(serviceId2, role.getId())
                .withUsername(username1)
                .withEmail(email1)
                .insertUser();
        String username2 = randomUuid();
        String email2 = username2 + "@example.com";
        User user2 = userDbFixture(databaseHelper)
                .withServiceRole(serviceId1, role.getId())
                .withServiceRole(serviceId2, role.getId())
                .withUsername(username2)
                .withEmail(email2)
                .insertUser();
        // Add third user to prove we're not just returning all users
        String username3 = randomUuid();
        String email3 = username3 + "@example.com";
        userDbFixture(databaseHelper)
                .withServiceRole(serviceId1, role.getId())
                .withServiceRole(serviceId2, role.getId())
                .withUsername(username3)
                .withEmail(email3)
                .insertUser();

        List<String> externalIds = Arrays.asList(user1.getExternalId(), user2.getExternalId());

        List<UserEntity> userEntities = userDao.findByExternalIds(externalIds);
        assertThat(userEntities.size(), is(2));

        UserEntity foundUser1 = userEntities.get(0);
        assertThat(foundUser1.getExternalId(), is(user1.getExternalId()));
        assertThat(foundUser1.getUsername(), is(user1.getUsername()));
        assertThat(foundUser1.getEmail(), is(user1.getEmail()));
        assertThat(foundUser1.getOtpKey(), is(user1.getOtpKey()));
        assertThat(foundUser1.getTelephoneNumber(), is("+447700900000"));
        assertThat(foundUser1.isDisabled(), is(false));
        assertThat(foundUser1.getLoginCounter(), is(0));
        assertThat(foundUser1.getSessionVersion(), is(0));
        assertThat(foundUser1.getRoles().size(), is(1));
        assertThat(foundUser1.toUser().getServiceRoles().size(), is(2));
        assertThat(foundUser1.getRoles().get(0).getId(), is(role.getId()));

        UserEntity foundUser2 = userEntities.get(1);
        assertThat(foundUser2.getExternalId(), is(user2.getExternalId()));
        assertThat(foundUser2.getUsername(), is(user2.getUsername()));
        assertThat(foundUser2.getEmail(), is(user2.getEmail()));
        assertThat(foundUser2.getOtpKey(), is(user2.getOtpKey()));
        assertThat(foundUser2.getTelephoneNumber(), is("+447700900000"));
        assertThat(foundUser2.isDisabled(), is(false));
        assertThat(foundUser2.getLoginCounter(), is(0));
        assertThat(foundUser2.getSessionVersion(), is(0));
        assertThat(foundUser2.getRoles().size(), is(1));
        assertThat(foundUser2.toUser().getServiceRoles().size(), is(2));
        assertThat(foundUser2.getRoles().get(0).getId(), is(role.getId()));
    }

    @Test
    public void shouldFindUserBy_Username_caseInsensitive() {
        Role role = roleDbFixture(databaseHelper).insertRole();
        int serviceId = serviceDbFixture(databaseHelper)
                .insertService().getId();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper)
                .withServiceRole(serviceId, role.getId()).withUsername(username).withEmail(email).insertUser();

        String otpKey = user.getOtpKey();

        Optional<UserEntity> userEntityMaybe = userDao.findByUsername(username.toUpperCase(Locale.ENGLISH));
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getEmail(), is(email));
        assertThat(foundUser.getUsername(), is(username));
        assertThat(foundUser.getOtpKey(), is(otpKey));
        assertThat(foundUser.getTelephoneNumber(), is("+447700900000"));
        assertThat(foundUser.isDisabled(), is(false));
        assertThat(foundUser.getLoginCounter(), is(0));
        assertThat(foundUser.getSessionVersion(), is(0));
        assertThat(foundUser.getRoles().size(), is(1));
        assertThat(foundUser.getRoles().get(0).getId(), is(role.getId()));
    }

    @Test
    public void shouldFindUser_ByEmail_caseInsensitive() {
        Role role = roleDbFixture(databaseHelper).insertRole();
        int serviceId = serviceDbFixture(databaseHelper).insertService().getId();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper)
                .withServiceRole(serviceId, role.getId()).withUsername(username).withEmail(email).insertUser();

        String otpKey = user.getOtpKey();

        Optional<UserEntity> userEntityMaybe = userDao.findByEmail(username + "@EXAMPLE.com");
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getUsername(), is(username));
        assertThat(foundUser.getEmail(), is(email));
        assertThat(foundUser.getOtpKey(), is(otpKey));
        assertThat(foundUser.getTelephoneNumber(), is("+447700900000"));
        assertThat(foundUser.isDisabled(), is(false));
        assertThat(foundUser.getLoginCounter(), is(0));
        assertThat(foundUser.getSessionVersion(), is(0));
        assertThat(foundUser.getRoles().size(), is(1));
        assertThat(foundUser.getRoles().get(0).getId(), is(role.getId()));
    }

    @Test
    public void shouldAddServiceRoleOfAnExistingUser_whenSettingANewServiceRole() {
        Role role1 = roleDbFixture(databaseHelper).insertRole();
        Role role2 = roleDbFixture(databaseHelper).insertRole();

        String gatewayAccountId1 = randomInt().toString();
        String gatewayAccountId2 = randomInt().toString();

        Service service1 = serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(gatewayAccountId1).insertService();
        serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(gatewayAccountId2).insertService();

        String username = randomUuid();
        String email = username + "@example.com";

        userDbFixture(databaseHelper)
                .withServiceRole(service1, role1.getId()).withUsername(username).withEmail(email).insertUser();

        UserEntity existingUser = userDao.findByUsername(username).get();

        assertThat(existingUser.getGatewayAccountId(), is(gatewayAccountId1));
        assertThat(existingUser.getRoles().size(), is(1));
        assertThat(existingUser.getRoles().get(0).getId(), is(role1.getId()));

        ServiceEntity serviceEntity2 = serviceDao.findByGatewayAccountId(gatewayAccountId2).get();
        RoleEntity roleEntity2 = roleDao.findByRoleName(role2.getName()).get();

        ServiceRoleEntity serviceRole = new ServiceRoleEntity(serviceEntity2, roleEntity2);
        serviceRole.setUser(existingUser);
        existingUser.addServiceRole(serviceRole);
        userDao.merge(existingUser);

        UserEntity changedUser = userDao.findByUsername(username).get();
        List<ServiceRoleEntity> servicesRoles = changedUser.getServicesRoles();
        assertThat(servicesRoles.size(), is(2));
        assertThat(servicesRoles.stream().map(sr -> sr.getService().getExternalId()).collect(toList()), hasItems(service1.getExternalId(), serviceEntity2.getExternalId()));
        assertThat(servicesRoles.stream().map(sr -> sr.getRole().getName()).collect(toList()), hasItems(role1.getName(), role2.getName()));
    }

    @Test
    public void shouldFindUsers_ByServiceId_OrderedByUsername() {

        Role role1 = roleDbFixture(databaseHelper)
                .withName("view")
                .insertRole();

        Role role2 = roleDbFixture(databaseHelper)
                .withName("admin")
                .insertRole();

        int serviceId = serviceDbFixture(databaseHelper).insertService().getId();

        String username1 = "thomas" + randomUuid();
        String email1 = username1 + "@example.com";
        User user1 = userDbFixture(databaseHelper)
                .withUsername(username1)
                .withEmail(email1)
                .withServiceRole(serviceId, role1.getId()).insertUser();

        String username2 = "bob" + randomUuid();
        String email2 = username2 + "@example.com";
        User user2 = userDbFixture(databaseHelper)
                .withUsername(username2)
                .withEmail(email2)
                .withServiceRole(serviceId, role2.getId()).insertUser();

        List<UserEntity> users = userDao.findByServiceId(serviceId);

        assertThat(users.size(), is(2));
        assertThat(users.get(0).getId(), is(user2.getId()));
        assertThat(users.get(1).getId(), is(user1.getId()));
    }

    @Test
    public void shouldNotFindAnyUser() {
        int serviceId = serviceDbFixture(databaseHelper).insertService().getId();

        List<UserEntity> users = userDao.findByServiceId(serviceId);

        assertThat(users.isEmpty(), is(true));
    }
}
