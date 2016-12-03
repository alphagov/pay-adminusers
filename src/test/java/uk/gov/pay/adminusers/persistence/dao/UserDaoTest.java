package uk.gov.pay.adminusers.persistence.dao;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

public class UserDaoTest extends DaoTestBase {

    private UserDao userDao;
    String random;
    Integer randomInt;

    @Before
    public void before() throws Exception {
        userDao = env.getInstance(UserDao.class);
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

        databaseTestHelper.add(perm1).add(perm2).add(perm3).add(perm4);
        databaseTestHelper.add(role1).add(role2);

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername("user-" + random);
        userEntity.setPassword("password-" + random);
        userEntity.setDisabled(false);
        userEntity.setEmail(random + "@example.com");
        userEntity.setGatewayAccountId(randomInt.toString());
        userEntity.setOtpKey(randomInt.toString());
        userEntity.setTelephoneNumber("876284762");
        userEntity.setRoles(asList(new RoleEntity(role1), new RoleEntity(role2)));

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

        databaseTestHelper.add(perm1).add(perm2).add(perm3);
        databaseTestHelper.add(role1).add(role2);

        String username = "user-" + random;
        User user = User.from(randomInt(), username, "password-" + random, random + "@example.com", randomInt.toString(), randomInt.toString(), "374628482");
        user.setRoles(asList(role1, role2));
        databaseTestHelper.add(user);

        Optional<UserEntity> userEntityMaybe = userDao.findByUsername(username);
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getEmail(), is(random + "@example.com"));
        assertThat(foundUser.getUsername(), is(username));
        assertThat(foundUser.getGatewayAccountId(), is(randomInt.toString()));
        assertThat(foundUser.getOtpKey(), is(randomInt.toString()));
        assertThat(foundUser.getTelephoneNumber(), is("374628482"));
        assertThat(foundUser.getDisabled(), is(false));
        assertThat(foundUser.getLoginCount(), is(0));
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

        databaseTestHelper.add(perm1).add(perm2).add(perm3);
        databaseTestHelper.add(role1).add(role2);

        String email = random + "@example.com";
        User user = User.from(randomInt(), "user-" + random, "password-" + random, email, randomInt.toString(), randomInt.toString(), "374628482");
        user.setRoles(asList(role1, role2));
        databaseTestHelper.add(user);

        Optional<UserEntity> userEntityMaybe = userDao.findByEmail(email);
        assertTrue(userEntityMaybe.isPresent());

        UserEntity foundUser = userEntityMaybe.get();
        assertThat(foundUser.getUsername(), is("user-" + random));
        assertThat(foundUser.getEmail(), is(email));
        assertThat(foundUser.getGatewayAccountId(), is(randomInt.toString()));
        assertThat(foundUser.getOtpKey(), is(randomInt.toString()));
        assertThat(foundUser.getTelephoneNumber(), is("374628482"));
        assertThat(foundUser.getDisabled(), is(false));
        assertThat(foundUser.getLoginCount(), is(0));
        assertThat(foundUser.getRoles().size(), is(2));
        assertThat(foundUser.getRoles().get(0).toRole(), either(is(role1)).or(is(role2)));
        assertThat(foundUser.getRoles().get(1).toRole(), either(is(role1)).or(is(role2)));
    }

    @Test
    public void shouldFindAUser_ByUserNamePasswordCombination() throws Exception {

        String username = "user-" + random;
        String password = "password-" + random;
        User user = User.from(randomInt(), username, password, random + "@example.com", randomInt.toString(), randomInt.toString(), "374628482");
        databaseTestHelper.add(user);

        Optional<UserEntity> userEntityOptional = userDao.findEnabledUserByUsernameAndPassword(username, password);
        assertTrue(userEntityOptional.isPresent());

        UserEntity foundUser = userEntityOptional.get();
        assertThat(foundUser.getUsername(), is("user-" + random));
        assertThat(foundUser.getEmail(), is(random + "@example.com"));
        assertThat(foundUser.getGatewayAccountId(), is(randomInt.toString()));
        assertThat(foundUser.getOtpKey(), is(randomInt.toString()));
        assertThat(foundUser.getTelephoneNumber(), is("374628482"));
        assertThat(foundUser.getDisabled(), is(false));
        assertThat(foundUser.getLoginCount(), is(0));

    }

    @Test
    public void shouldNotFindUser_ifInvalidUserNamePasswordCombination() throws Exception {
        String username = "user-" + random;
        String password = "password-" + random;
        User user = User.from(newLongId(), username, password, random + "@example.com", randomLong.toString(), randomLong.toString(), "374628482");
        databaseTestHelper.add(user);

        Optional<UserEntity> userEntityOptional2 = userDao.findEnabledUserByUsernameAndPassword(username, "invalid-password");
        assertFalse(userEntityOptional2.isPresent());
    }

    @Test
    public void shouldNotFindUser_ByUserNamePasswordCombination_ifDisabled() throws Exception {

        String username = "user-" + random;
        String password = "password-" + random;
        User user = User.from(newLongId(), username, password, random + "@example.com", randomLong.toString(), randomLong.toString(), "374628482");
        user.setDisabled(true);
        databaseTestHelper.add(user);

        Optional<UserEntity> userEntityOptional = userDao.findEnabledUserByUsernameAndPassword(username, password);
        assertFalse(userEntityOptional.isPresent());

    }

}
