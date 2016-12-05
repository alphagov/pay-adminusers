package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Link;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import javax.persistence.RollbackException;
import javax.ws.rs.WebApplicationException;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.pay.adminusers.resources.UserResource.USERS_RESOURCE;
import static uk.gov.pay.adminusers.service.UserServices.CONSTRAINT_VIOLATION_MESSAGE;

public class UserServicesTest {

    private UserDao userDao;
    private RoleDao roleDao;
    private PasswordHasher passwordHasher;
    private UserServices userServices;
    private LinksBuilder linksBuilder;

    @Before
    public void before() throws Exception {
        userDao = mock(UserDao.class);
        roleDao = mock(RoleDao.class);
        passwordHasher = mock(PasswordHasher.class);
        linksBuilder = new LinksBuilder("http://localhost");
        userServices = new UserServices(userDao, roleDao, passwordHasher, linksBuilder);
    }

    @Test(expected = WebApplicationException.class)
    public void shouldError_ifRoleNameDoesNotExist() throws Exception {
        User user = aUser();
        String nonExistentRole = "nonExistentRole";
        when(roleDao.findByRoleName(nonExistentRole)).thenReturn(Optional.empty());
        userServices.createUser(user, nonExistentRole);
    }

    @Test(expected = WebApplicationException.class)
    public void shouldError_ifRoleNameConflicts() throws Exception {
        User user = aUser();
        Role role = Role.role(2l, "admin", "admin role");

        when(roleDao.findByRoleName(role.getName())).thenReturn(Optional.of(new RoleEntity(role)));
        when(passwordHasher.hash("random-password")).thenReturn("the hashed random-password");
        doThrow(new RollbackException(CONSTRAINT_VIOLATION_MESSAGE)).when(userDao).persist(any(UserEntity.class));

        userServices.createUser(user, role.getName());
    }

    @Test(expected = WebApplicationException.class)
    public void shouldError_ifUnknownErrorThrownWhenSaving() throws Exception {
        User user = aUser();
        Role role = Role.role(2l, "admin", "admin role");

        when(roleDao.findByRoleName(role.getName())).thenReturn(Optional.of(new RoleEntity(role)));
        when(passwordHasher.hash("random-password")).thenReturn("the hashed random-password");
        doThrow(new RuntimeException("unknown error")).when(userDao).persist(any(UserEntity.class));

        userServices.createUser(user, role.getName());
    }

    @Test
    public void shouldPersistAUserSuccessfully() throws Exception {
        User user = aUser();
        Role role = Role.role(2l, "admin", "admin role");

        when(roleDao.findByRoleName(role.getName())).thenReturn(Optional.of(new RoleEntity(role)));
        when(passwordHasher.hash("random-password")).thenReturn("the hashed random-password");
        doNothing().when(userDao).persist(any(UserEntity.class));

        User persistedUser = userServices.createUser(user, role.getName());
        Link selfLink = Link.from(Link.Rel.self, "GET", "http://localhost" + USERS_RESOURCE + "/random-name");

        assertThat(persistedUser.getUsername(), is(user.getUsername()));
        assertThat(persistedUser.getPassword(), is(not(user.getPassword())));
        assertThat(persistedUser.getEmail(), is(user.getEmail()));
        assertThat(persistedUser.getGatewayAccountId(), is(user.getGatewayAccountId()));
        assertThat(persistedUser.getTelephoneNumber(), is(user.getTelephoneNumber()));
        assertThat(persistedUser.getOtpKey(), is(user.getOtpKey()));
        assertThat(persistedUser.getRoles().size(), is(1));
        assertThat(persistedUser.getRoles().get(0), is(role));
        assertThat(persistedUser.getLinks().get(0), is(selfLink));
    }

    @Test
    public void shouldFindAUserByUserName() throws Exception {
        User user = aUser();

        Optional<UserEntity> userEntityOptional = Optional.of(UserEntity.from(user));
        when(userDao.findByUsername("random-name")).thenReturn(userEntityOptional);

        Optional<User> userOptional = userServices.findUser("random-name");
        assertTrue(userOptional.isPresent());

        assertThat(userOptional.get().getUsername(),is("random-name"));
    }

    @Test
    public void shouldReturnEmpty_WhenFindByUserName_ifNotFound() throws Exception {
        when(userDao.findByUsername("random-name")).thenReturn(Optional.empty());

        Optional<User> userOptional = userServices.findUser("random-name");
        assertFalse(userOptional.isPresent());
    }

    private User aUser() {
        return User.from("random-name", "random-password", "random@email.com", "1", "784rh", "8948924");
    }

}
