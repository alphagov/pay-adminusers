package uk.gov.pay.adminusers.persistence.dao;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;

import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.model.Role.role;

public class ServiceDaoTest extends DaoTestBase {

    private ServiceDao serviceDao;

    @Before
    public void before() throws Exception {
        serviceDao = env.getInstance(ServiceDao.class);
    }

    @Test
    public void shouldGetRoleCountForAService() throws Exception {
        Integer serviceId = randomInt();
        Integer roleId = randomInt();
        setupUsersForServiceAndRole(serviceId, roleId, 3);

        Long count = serviceDao.countOfRolesForService(serviceId, roleId);

        assertThat(count, is(3l));

    }

    private void setupUsersForServiceAndRole(int serviceId, int roleId, int noOfUsers) {
        Permission perm1 = aPermission();
        Permission perm2 = aPermission();
        databaseHelper.add(perm1).add(perm2);

        Role role = role(roleId, "role-" + roleId, "role-desc-" + roleId);
        role.setPermissions(asList(perm1, perm2));
        databaseHelper.add(role);

        String gatewayAccountId1 = randomInt().toString();
        Service service1 = Service.from(serviceId, Service.DEFAULT_NAME_VALUE);
        databaseHelper.addService(service1, gatewayAccountId1);

        range(0, noOfUsers - 1).forEach(i -> {
            UserDbFixture.userDbFixture(databaseHelper).withServiceRole(serviceId, roleId).insertUser();
        });

        //unmatching service
        String gatewayAccountId2 = randomInt().toString();
        Integer serviceId2 = randomInt();
        Service service2 = Service.from(serviceId2, Service.DEFAULT_NAME_VALUE);
        databaseHelper.addService(service2, gatewayAccountId2);

        //same user 2 diff services - should count only once
        User user3 = UserDbFixture.userDbFixture(databaseHelper).withServiceRole(serviceId, roleId).insertUser();
        databaseHelper.addUserServiceRole(user3.getId(),serviceId2, role.getId());
    }


}
