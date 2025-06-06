package uk.gov.pay.adminusers.persistence.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.RoleName;
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
import java.util.stream.Collectors;

import static java.lang.String.valueOf;
import static java.time.ZonedDateTime.parse;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class UserDaoIT extends DaoTestBase {

    private UserDao userDao;
    private ServiceDao serviceDao;
    private RoleDao roleDao;
    private Role adminRole;
    private Role viewOnlyRole;

    @BeforeEach
    public void before() {
        userDao = env.getInstance(UserDao.class);
        serviceDao = env.getInstance(ServiceDao.class);
        roleDao = env.getInstance(RoleDao.class);
        databaseHelper.truncateAllData();
        adminRole = roleDao.findByRoleName(RoleName.ADMIN).get().toRole();
        viewOnlyRole = roleDao.findByRoleName(RoleName.VIEW_ONLY).get().toRole();
    }

    @Test
    void getAdminUserEmailsForGatewayAccountIds_should_return_empty_map() {
        Map<String, List<String>> map = userDao.getAdminUserEmailsForGatewayAccountIds(List.of());
        assertThat(map.isEmpty(), is(true));
    }

    @Test
    public void shouldCreateAUserSuccessfully() {
        String gatewayAccountId = randomInt().toString();
        int serviceId = serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(gatewayAccountId).insertService().getId();

        String username = valueOf(nextInt());

        UserEntity userEntity = new UserEntity();
        userEntity.setExternalId(randomUuid());
        userEntity.setPassword("password-" + username);
        userEntity.setDisabled(false);
        userEntity.setEmail(username + "@example.com");
        userEntity.setOtpKey(randomInt().toString());
        userEntity.setTelephoneNumber("+447700900000");
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setSessionVersion(0);
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(MICROS);
        userEntity.setCreatedAt(timeNow);
        userEntity.setUpdatedAt(timeNow);

        ServiceEntity serviceEntity = serviceDao.findByGatewayAccountId(gatewayAccountId).get();
        RoleEntity adminRoleEntity = new RoleEntity(adminRole);

        ServiceRoleEntity serviceRoleEntity = new ServiceRoleEntity(serviceEntity, adminRoleEntity);
        serviceRoleEntity.setUser(userEntity);

        userEntity.addServiceRole(serviceRoleEntity);

        userDao.persist(userEntity);

        assertThat(userEntity.getId(), is(notNullValue()));
        List<Map<String, Object>> savedUserData = databaseHelper.findUser(userEntity.getId());
        assertThat(savedUserData.size(), is(1));
        assertThat((String) savedUserData.get(0).get("external_id"), not(emptyOrNullString()));
        assertThat(((String) savedUserData.get(0).get("external_id")).length(), equalTo(32));
        assertThat(savedUserData.get(0).get("password"), is(userEntity.getPassword()));
        assertThat(savedUserData.get(0).get("email"), is(userEntity.getEmail()));
        assertThat(savedUserData.get(0).get("otp_key"), is(userEntity.getOtpKey()));
        assertThat(savedUserData.get(0).get("telephone_number"), is(userEntity.getTelephoneNumber().get()));
        assertThat(savedUserData.get(0).get("disabled"), is(Boolean.FALSE));
        assertThat(savedUserData.get(0).get("session_version"), is(0));
        assertThat(savedUserData.get(0).get("createdat"), is(java.sql.Timestamp.from(timeNow.toInstant())));
        assertThat(savedUserData.get(0).get("updatedat"), is(java.sql.Timestamp.from(timeNow.toInstant())));

        List<Map<String, Object>> serviceRolesForUser = databaseHelper.findServiceRoleForUser(userEntity.getId());
        assertThat(serviceRolesForUser.size(), is(1));
        assertThat(serviceRolesForUser.get(0).get("id"), is(adminRoleEntity.getId()));
        assertThat(serviceRolesForUser.get(0).get("service_id"), is(serviceId));
        assertThat(serviceRolesForUser.get(0).get("name"), is(adminRoleEntity.getRoleName().getName()));
        assertThat(serviceRolesForUser.get(0).get("description"), is(adminRoleEntity.getDescription()));
    }

    @Test
    public void shouldFindUserBy_ExternalId() {
        int serviceId1 = serviceDbFixture(databaseHelper)
                .insertService().getId();
        int serviceId2 = serviceDbFixture(databaseHelper)
                .insertService().getId();
        String email = randomUuid() + "@example.com";
        User user = userDbFixture(databaseHelper)
                .withServiceRole(serviceId1, adminRole)
                .withServiceRole(serviceId2, adminRole)
                .withEmail(email)
                .insertUser();

        String externalId = user.getExternalId();
        String otpKey = user.getOtpKey();

        Optional<UserEntity> userEntityMaybe = userDao.findByExternalId(externalId);
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getExternalId(), is(externalId));
        assertThat(foundUser.getEmail(), is(email));
        assertThat(foundUser.getOtpKey(), is(otpKey));
        assertThat(foundUser.getTelephoneNumber().get(), is("+447700900000"));
        assertThat(foundUser.isDisabled(), is(false));
        assertThat(foundUser.getLoginCounter(), is(0));
        assertThat(foundUser.getSessionVersion(), is(0));
        assertThat(foundUser.getRoles().size(), is(1));
        assertThat(foundUser.toUser().getServiceRoles().size(), is(2));
        assertThat(foundUser.getRoles().get(0).getId(), is(adminRole.getId()));
    }

    @Test
    public void shouldFindUsersBy_ExternalIds() {
        int serviceId1 = serviceDbFixture(databaseHelper)
                .insertService().getId();
        int serviceId2 = serviceDbFixture(databaseHelper)
                .insertService().getId();
        String username1 = randomUuid();
        String email1 = username1 + "@example.com";
        User user1 = userDbFixture(databaseHelper)
                .withServiceRole(serviceId1, adminRole)
                .withServiceRole(serviceId2, adminRole)
                .withEmail(email1)
                .insertUser();
        String username2 = randomUuid();
        String email2 = username2 + "@example.com";
        User user2 = userDbFixture(databaseHelper)
                .withServiceRole(serviceId1, adminRole)
                .withServiceRole(serviceId2, adminRole)
                .withEmail(email2)
                .insertUser();
        // Add third user to prove we're not just returning all users
        String username3 = randomUuid();
        String email3 = username3 + "@example.com";
        userDbFixture(databaseHelper)
                .withServiceRole(serviceId1, adminRole)
                .withServiceRole(serviceId2, adminRole)
                .withEmail(email3)
                .insertUser();

        List<String> externalIds = Arrays.asList(user1.getExternalId(), user2.getExternalId());

        List<UserEntity> userEntities = userDao.findByExternalIds(externalIds);
        assertThat(userEntities.size(), is(2));

        UserEntity foundUser1 = userEntities.get(0);
        assertThat(foundUser1.getExternalId(), is(user1.getExternalId()));
        assertThat(foundUser1.getEmail(), is(user1.getEmail()));
        assertThat(foundUser1.getOtpKey(), is(user1.getOtpKey()));
        assertThat(foundUser1.getTelephoneNumber().get(), is("+447700900000"));
        assertThat(foundUser1.isDisabled(), is(false));
        assertThat(foundUser1.getLoginCounter(), is(0));
        assertThat(foundUser1.getSessionVersion(), is(0));
        assertThat(foundUser1.getRoles().size(), is(1));
        assertThat(foundUser1.toUser().getServiceRoles().size(), is(2));
        assertThat(foundUser1.getRoles().get(0).getId(), is(adminRole.getId()));

        UserEntity foundUser2 = userEntities.get(1);
        assertThat(foundUser2.getExternalId(), is(user2.getExternalId()));
        assertThat(foundUser2.getEmail(), is(user2.getEmail()));
        assertThat(foundUser2.getOtpKey(), is(user2.getOtpKey()));
        assertThat(foundUser2.getTelephoneNumber().get(), is("+447700900000"));
        assertThat(foundUser2.isDisabled(), is(false));
        assertThat(foundUser2.getLoginCounter(), is(0));
        assertThat(foundUser2.getSessionVersion(), is(0));
        assertThat(foundUser2.getRoles().size(), is(1));
        assertThat(foundUser2.toUser().getServiceRoles().size(), is(2));
        assertThat(foundUser2.getRoles().get(0).getId(), is(adminRole.getId()));
    }

    @Test
    public void shouldFindUserBy_UserEmail_caseInsensitive() {
        int serviceId = serviceDbFixture(databaseHelper)
                .insertService().getId();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper)
                .withServiceRole(serviceId, adminRole).withEmail(email).insertUser();

        String otpKey = user.getOtpKey();

        Optional<UserEntity> userEntityMaybe = userDao.findByEmail(email.toLowerCase(Locale.ENGLISH));
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getEmail(), is(email));
        assertThat(foundUser.getOtpKey(), is(otpKey));
        assertThat(foundUser.getTelephoneNumber().get(), is("+447700900000"));
        assertThat(foundUser.isDisabled(), is(false));
        assertThat(foundUser.getLoginCounter(), is(0));
        assertThat(foundUser.getSessionVersion(), is(0));
        assertThat(foundUser.getRoles().size(), is(1));
        assertThat(foundUser.getRoles().get(0).getId(), is(adminRole.getId()));
    }

    @Test
    public void shouldFindUser_ByEmail_caseInsensitive() {
        int serviceId = serviceDbFixture(databaseHelper).insertService().getId();
        String username = randomUuid();
        String email = username + "@example.com";
        User user = userDbFixture(databaseHelper)
                .withServiceRole(serviceId, adminRole).withEmail(email).insertUser();

        String otpKey = user.getOtpKey();

        Optional<UserEntity> userEntityMaybe = userDao.findByEmail(username + "@EXAMPLE.com");
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getEmail(), is(email));
        assertThat(foundUser.getOtpKey(), is(otpKey));
        assertThat(foundUser.getTelephoneNumber().get(), is("+447700900000"));
        assertThat(foundUser.isDisabled(), is(false));
        assertThat(foundUser.getLoginCounter(), is(0));
        assertThat(foundUser.getSessionVersion(), is(0));
        assertThat(foundUser.getRoles().size(), is(1));
        assertThat(foundUser.getRoles().get(0).getId(), is(adminRole.getId()));
    }

    @Test
    public void shouldAddServiceRoleOfAnExistingUser_whenSettingANewServiceRole() {
        String gatewayAccountId1 = randomInt().toString();
        String gatewayAccountId2 = randomInt().toString();

        Service service1 = serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(gatewayAccountId1).insertService();
        serviceDbFixture(databaseHelper)
                .withGatewayAccountIds(gatewayAccountId2).insertService();

        String username = randomUuid();
        String email = username + "@example.com";

        userDbFixture(databaseHelper).withServiceRole(service1, adminRole).withEmail(email).insertUser();

        UserEntity existingUser = userDao.findByEmail(email).get();

        assertThat(existingUser.getGatewayAccountId(), is(gatewayAccountId1));
        assertThat(existingUser.getRoles().size(), is(1));
        assertThat(existingUser.getRoles().get(0).getId(), is(adminRole.getId()));

        ServiceEntity serviceEntity2 = serviceDao.findByGatewayAccountId(gatewayAccountId2).get();
        RoleEntity roleEntity2 = new RoleEntity(viewOnlyRole);

        ServiceRoleEntity serviceRole = new ServiceRoleEntity(serviceEntity2, roleEntity2);
        serviceRole.setUser(existingUser);
        existingUser.addServiceRole(serviceRole);
        userDao.merge(existingUser);

        UserEntity changedUser = userDao.findByEmail(email).get();
        List<ServiceRoleEntity> servicesRoles = changedUser.getServicesRoles();
        assertThat(servicesRoles.size(), is(2));
        assertThat(servicesRoles.stream().map(sr -> sr.getService().getExternalId()).collect(toUnmodifiableList()),
                hasItems(service1.getExternalId(), serviceEntity2.getExternalId()));
        assertThat(servicesRoles.stream().map(sr -> sr.getRole().getRoleName()).collect(toUnmodifiableList()),
                hasItems(adminRole.getRoleName(), viewOnlyRole.getRoleName()));
    }

    @Test
    public void shouldFindUsers_ByServiceId_OrderedByUsername() {

        int serviceId = serviceDbFixture(databaseHelper).insertService().getId();

        String username1 = "thomas" + randomUuid();
        String email1 = username1 + "@example.com";
        User user1 = userDbFixture(databaseHelper)
                .withEmail(email1)
                .withServiceRole(serviceId, adminRole).insertUser();

        String username2 = "bob" + randomUuid();
        String email2 = username2 + "@example.com";
        User user2 = userDbFixture(databaseHelper)
                .withEmail(email2)
                .withServiceRole(serviceId, adminRole).insertUser();

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

    @Test
    public void shouldNotCreateAUserWithDifferentCaseEmail() {
        int serviceId = serviceDbFixture(databaseHelper).insertService().getId();
        userDbFixture(databaseHelper)
                .withEmail("user@example.com")
                .withServiceRole(serviceId, adminRole).insertUser();

        String username = valueOf(nextInt());

        UserEntity userEntity = new UserEntity();
        userEntity.setExternalId(randomUuid());
        userEntity.setPassword("password-" + username);
        userEntity.setDisabled(false);
        userEntity.setEmail("User@example.com");
        userEntity.setOtpKey(randomInt().toString());
        userEntity.setTelephoneNumber("+447700900000");
        userEntity.setSecondFactor(SecondFactorMethod.SMS);
        userEntity.setSessionVersion(0);
        ZonedDateTime timeNow = ZonedDateTime.now(ZoneId.of("UTC"));
        userEntity.setCreatedAt(timeNow);
        userEntity.setUpdatedAt(timeNow);

        Optional<ServiceEntity> serviceEntity = serviceDao.findById(serviceId);
        RoleEntity roleEntity = roleDao.findByRoleName(RoleName.VIEW_REFUND_AND_INITIATE_MOTO).get();

        ServiceRoleEntity serviceRoleEntity = new ServiceRoleEntity(serviceEntity.get(), roleEntity);
        serviceRoleEntity.setUser(userEntity);

        userEntity.addServiceRole(serviceRoleEntity);

        var thrown = assertThrows(jakarta.persistence.RollbackException.class, () -> userDao.persist(userEntity));
        assertThat(thrown.getMessage(), containsString("ERROR: duplicate key value violates unique constraint \"lower_case_email_index\""));
    }

    @Nested
    class TestDeleteUsersNotAssociatedWithAnyService {

        @Test
        void shouldDeleteUsersWithLastLoggedInAtDateCorrectly() {
            ZonedDateTime deleteUsersUpToDate = parse("2023-01-31T00:00:00Z");
            Integer userId = userDbFixture(databaseHelper)
                    .withLastLoggedInAt(deleteUsersUpToDate.minusDays(1))
                    .insertUser()
                    .getId();
            Integer userIdThatShouldNotDeleted = userDbFixture(databaseHelper)
                    .withLastLoggedInAt(deleteUsersUpToDate.plusDays(1))
                    .insertUser()
                    .getId();

            int recordsDeleted = userDao.deleteUsersNotAssociatedWithAnyService(deleteUsersUpToDate.toInstant());

            assertThat(recordsDeleted, is(1));

            Optional<UserEntity> userEntity = userDao.findById(userId);
            assertThat(userEntity.isPresent(), is(false));

            userEntity = userDao.findById(userIdThatShouldNotDeleted);
            assertThat(userEntity.isPresent(), is(true));
        }

        @Test
        void shouldDeleteUsersWithoutLastLoggedInAtDateCorrectlyButCreatedDateBeforeTheExpungeDate() {
            ZonedDateTime deleteUsersUpToDate = parse("2023-01-31T00:00:00Z");
            Integer userId = userDbFixture(databaseHelper)
                    .withLastLoggedInAt(null)
                    .withCreatedAt(deleteUsersUpToDate.minusDays(1))
                    .insertUser()
                    .getId();
            Integer userIdThatShouldNotDeleted = userDbFixture(databaseHelper)
                    .withLastLoggedInAt(null)
                    .withCreatedAt(deleteUsersUpToDate.plusDays(1))
                    .insertUser()
                    .getId();

            int recordsDeleted = userDao.deleteUsersNotAssociatedWithAnyService(deleteUsersUpToDate.toInstant());

            assertThat(recordsDeleted, is(1));

            Optional<UserEntity> userEntity = userDao.findById(userId);
            assertThat(userEntity.isPresent(), is(false));

            userEntity = userDao.findById(userIdThatShouldNotDeleted);
            assertThat(userEntity.isPresent(), is(true));
        }

        @Test
        void shouldNotDeleteUsersWithLoggedInOrCreatedIfDateIsAfterTheExpungingDate() {
            ZonedDateTime deleteUsersUpToDate = parse("2023-01-31T00:00:00Z");
            String externalId1 = userDbFixture(databaseHelper)
                    .withLastLoggedInAt(deleteUsersUpToDate.plusDays(1))
                    .insertUser()
                    .getExternalId();
            String externalId2 = userDbFixture(databaseHelper)
                    .withLastLoggedInAt(null)
                    .withCreatedAt(deleteUsersUpToDate.plusDays(1))
                    .insertUser()
                    .getExternalId();

            int recordsDeleted = userDao.deleteUsersNotAssociatedWithAnyService(deleteUsersUpToDate.toInstant());

            assertThat(recordsDeleted, is(0));

            List<UserEntity> users = userDao.findByExternalIds(List.of(externalId1, externalId2));

            assertThat(users.size(), is(2));
            assertThat(users.stream().map(UserEntity::getExternalId).collect(Collectors.toSet()), containsInAnyOrder(externalId1, externalId2));
        }
    }
}
