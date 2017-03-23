package uk.gov.pay.adminusers.persistence.dao;

import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.core.IsNot;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.ServiceDbFixture;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import java.util.Map;
import java.util.Optional;

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

    @Test
    public void shouldChangeServiceName() throws Exception {
        Integer serviceId = ServiceDbFixture.serviceDbFixture(databaseHelper).insertService();
        String newServiceName = RandomStringUtils.randomAlphanumeric(20);

        Optional<ServiceEntity> optionalServiceEntity = serviceDao.updateServiceName(serviceId, newServiceName);
        Map<String, Object> finds = databaseHelper.findServiceByServiceId(serviceId);
        //TODO why does it differ?
        assertThat(finds.get("name"), is(newServiceName));
        assertThat(optionalServiceEntity.isPresent(), is(true));
        assertThat(optionalServiceEntity.get().getName(), is(newServiceName));

    }

    private void setupUsersForServiceAndRole(int serviceId, int roleId, int noOfUsers) {

        Permission perm1 = aPermission();
        Permission perm2 = aPermission();
        databaseHelper.add(perm1).add(perm2);

        Role role = role(roleId, "role-" + roleId, "role-desc-" + roleId);
        role.setPermissions(asList(perm1, perm2));
        databaseHelper.add(role);

        String gatewayAccountId1 = randomInt().toString();
        databaseHelper.addService(serviceId, gatewayAccountId1);

        range(0, noOfUsers - 1).forEach(i -> {
            UserDbFixture.userDbFixture(databaseHelper).withServiceRole(serviceId, roleId).insertUser();
        });

        //unmatching service
        String gatewayAccountId2 = randomInt().toString();
        Integer serviceId2 = randomInt();
        databaseHelper.addService(serviceId2, gatewayAccountId2);

        //same user 2 diff services - should count only once
        User user3 = UserDbFixture.userDbFixture(databaseHelper).withServiceRole(serviceId, roleId).insertUser();
        databaseHelper.addUserServiceRole(user3.getId(),serviceId2, role.getId());
    }


}
