package uk.gov.pay.adminusers.persistence.dao;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;
import uk.gov.pay.adminusers.model.*;
import uk.gov.pay.adminusers.persistence.entity.ServiceCustomisationEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertTrue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.Role.role;

public class ServiceDaoTest extends DaoTestBase {

    private ServiceDao serviceDao;

    @Before
    public void before() throws Exception {
        serviceDao = env.getInstance(ServiceDao.class);
    }


    @Test
    public void shouldSaveAService_withCustomisations() throws Exception {
        ServiceCustomisationEntity customisationEntity = new ServiceCustomisationEntity();
        customisationEntity.setBannerColour("red");
        customisationEntity.setLogoUrl("http://some.random.url/");
        customisationEntity.setUpdated(now());

        ServiceEntity serviceEntity = new ServiceEntity();
        String serviceExternalId = randomUuid();
        serviceEntity.setExternalId(serviceExternalId);
        serviceEntity.setName("random name");
        serviceEntity.setServiceCustomisationEntity(customisationEntity);

        serviceDao.persist(serviceEntity);

        List<Map<String, Object>> savedService = databaseHelper.findServiceByExternalId(serviceExternalId);

        assertThat(savedService.size(), is(1));
        assertThat(savedService.get(0).get("external_id"), is(serviceExternalId));
        assertThat(savedService.get(0).get("customisations_id"), is(notNullValue()));

        List<Map<String, Object>> savedCustomisations = databaseHelper.findServiceCustomisationsByServiceExternalId(serviceExternalId);
        assertThat(savedCustomisations.size(), is(1));
        assertThat(savedCustomisations.get(0).get("banner_colour"), is("red"));
        assertThat(savedCustomisations.get(0).get("logo_url"), is("http://some.random.url/"));
    }

    @Test
    public void shouldFindByServiceExternalId() throws Exception {
        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "name");
        ServiceCustomisations customisations = new ServiceCustomisations("red","url");
        service.setServiceCustomisations(customisations);
        databaseHelper.addService(service, randomInt().toString());

        Optional<ServiceEntity> serviceEntity = serviceDao.findByExternalId(serviceExternalId);

        assertTrue(serviceEntity.isPresent());
        assertThat(serviceEntity.get().getId(),is(service.getId()));
        assertThat(serviceEntity.get().getName(),is("name"));
        assertThat(serviceEntity.get().getServiceCustomisationEntity().getBannerColour(),is("red"));
        assertThat(serviceEntity.get().getServiceCustomisationEntity().getLogoUrl(),is("url"));
    }


    @Test
    public void shouldFindByGatewayAccountId() throws Exception {

        String gatewayAccountId = randomInt().toString();
        Integer serviceId = randomInt();
        String serviceExternalId = randomUuid();
        String name = "name";
        databaseHelper.addService(Service.from(serviceId, serviceExternalId, name), gatewayAccountId);

        Optional<ServiceEntity> optionalService = serviceDao.findByGatewayAccountId(gatewayAccountId);

        assertThat(optionalService.isPresent(), is(true));
        assertThat(optionalService.get().getExternalId(), is(serviceExternalId));
        assertThat(optionalService.get().getName(), is(name));

    }

    @Test
    public void shouldGetRoleCountForAService() throws Exception {
        String serviceExternalId = randomUuid();
        Integer roleId = randomInt();
        setupUsersForServiceAndRole(serviceExternalId, roleId, 3);

        Long count = serviceDao.countOfUsersWithRoleForService(serviceExternalId, roleId);

        assertThat(count, is(3l));

    }

    private void setupUsersForServiceAndRole(String externalId, int roleId, int noOfUsers) {
        Permission perm1 = aPermission();
        Permission perm2 = aPermission();
        databaseHelper.add(perm1).add(perm2);

        Role role = role(roleId, "role-" + roleId, "role-desc-" + roleId);
        role.setPermissions(asList(perm1, perm2));
        databaseHelper.add(role);

        String gatewayAccountId1 = randomInt().toString();
        Service service1 = Service.from(randomInt(), externalId, Service.DEFAULT_NAME_VALUE);
        databaseHelper.addService(service1, gatewayAccountId1);

        range(0, noOfUsers - 1).forEach(i -> {
            UserDbFixture.userDbFixture(databaseHelper).withServiceRole(service1, roleId).insertUser();
        });

        //unmatching service
        String gatewayAccountId2 = randomInt().toString();
        Integer serviceId2 = randomInt();
        String externalId2 = randomUuid();
        Service service2 = Service.from(serviceId2, externalId2, Service.DEFAULT_NAME_VALUE);
        databaseHelper.addService(service2, gatewayAccountId2);

        //same user 2 diff services - should count only once
        User user3 = UserDbFixture.userDbFixture(databaseHelper).withServiceRole(service1, roleId).insertUser();
        databaseHelper.addUserServiceRole(user3.getId(), serviceId2, role.getId());
    }


}
