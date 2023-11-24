package uk.gov.pay.adminusers.persistence.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.fixtures.RoleDbFixture;
import uk.gov.pay.adminusers.fixtures.ServiceDbFixture;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserServiceId;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

class ServiceRoleDaoIT extends DaoTestBase {

    private ServiceRoleDao serviceRoleDao;

    @BeforeEach
    public void before() {
        serviceRoleDao = env.getInstance(ServiceRoleDao.class);
    }

    @Test
    void shouldRemoveAServiceRoleOfAUserSuccessfully() {

        Service service = ServiceDbFixture.serviceDbFixture(databaseHelper).insertService();
        int roleId = RoleDbFixture.roleDbFixture(databaseHelper).insertRole().getId();
        String username = randomUuid();
        String email = username + "@example.com";
        User user =
                UserDbFixture.userDbFixture(databaseHelper)
                        .withServiceRole(service, roleId)
                        .withEmail(email)
                        .insertUser();
        UserServiceId userServiceId = new UserServiceId();
        userServiceId.setServiceId(user.getServiceRoles().get(0).getService().getId());
        userServiceId.setUserId(user.getId());

        List<Map<String, Object>> serviceRoles = databaseHelper.findServiceRoleForUser(user.getId());
        assertThat(serviceRoles.size(), is(1));

        ServiceRoleEntity serviceRoleOfUser = serviceRoleDao.findById(userServiceId).get();

        serviceRoleDao.remove(serviceRoleOfUser);

        List<Map<String, Object>> serviceRolesAfterRemove = databaseHelper.findServiceRoleForUser(user.getId());

        assertThat(serviceRolesAfterRemove.size(), is(0));
    }

    @Test
    void findServiceUserRoles_ShouldReturnRolesCorrectly() {
        Service serviceToFind = ServiceDbFixture.serviceDbFixture(databaseHelper).insertService();
        int roleId = RoleDbFixture.roleDbFixture(databaseHelper).insertRole().getId();

        User user = UserDbFixture.userDbFixture(databaseHelper)
                .withServiceRole(serviceToFind, roleId)
                .insertUser();
        User user2 = UserDbFixture.userDbFixture(databaseHelper)
                .withServiceRole(serviceToFind, roleId)
                .insertUser();

        Service service2 = ServiceDbFixture.serviceDbFixture(databaseHelper).insertService();
        User userThatShouldNotBeReturned = UserDbFixture.userDbFixture(databaseHelper)
                .withServiceRole(service2, roleId)
                .insertUser();

        List<ServiceRoleEntity> serviceUserRoles = serviceRoleDao.findServiceUserRoles(serviceToFind.getId());

        assertThat(serviceUserRoles.size(), is(2));
        assertThat(serviceUserRoles.stream().map(serviceRoleEntity -> serviceRoleEntity.getUser().getExternalId()).collect(Collectors.toList()),
                containsInAnyOrder(user.getExternalId(), user2.getExternalId()));
    }
}
