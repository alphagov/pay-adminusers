package uk.gov.pay.adminusers.persistence.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.postgresql.util.PGobject;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;
import uk.gov.pay.adminusers.model.MerchantDetails;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.CustomBrandingConverter;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.Role.role;

public class ServiceDaoTest extends DaoTestBase {

    private ServiceDao serviceDao;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void before() throws Exception {
        serviceDao = env.getInstance(ServiceDao.class);
    }


    @Test
    public void shouldSaveAService_withCustomisations() throws Exception {
        ServiceEntity serviceEntity = new ServiceEntity();
        String serviceExternalId = randomUuid();
        serviceEntity.setExternalId(serviceExternalId);
        serviceEntity.setName("random name");
        Map<String, Object> customBranding = ImmutableMap.of("image_url", "image url");
        serviceEntity.setCustomBranding(customBranding);

        serviceDao.persist(serviceEntity);

        List<Map<String, Object>> savedService = databaseHelper.findServiceByExternalId(serviceExternalId);

        assertThat(savedService.size(), is(1));
        assertThat(savedService.get(0).get("external_id"), is(serviceExternalId));
        Map<String, Object> storedBranding = objectMapper.readValue(savedService.get(0).get("custom_branding").toString(), new TypeReference<Map<String, Object>>() {
        });
        assertThat(storedBranding, is(customBranding));
        assertThat(storedBranding.keySet().size(), is(1));
        assertThat(storedBranding.keySet(), hasItems("image_url"));
        assertThat(storedBranding.values(), hasItems("image url"));
    }

    @Test
    public void shouldSaveAService_withoutCustomisations() throws Exception {
        ServiceEntity serviceEntity = new ServiceEntity();
        String serviceExternalId = randomUuid();
        serviceEntity.setExternalId(serviceExternalId);
        serviceEntity.setName("random name");

        serviceDao.persist(serviceEntity);

        List<Map<String, Object>> savedService = databaseHelper.findServiceByExternalId(serviceExternalId);

        assertThat(savedService.size(), is(1));
        assertThat(savedService.get(0).get("external_id"), is(serviceExternalId));
        Map<String, Object> storedBranding = new CustomBrandingConverter().convertToEntityAttribute((PGobject) savedService.get(0).get("custom_branding"));
        assertNull(storedBranding);
    }

    @Test
    public void shouldSaveAService_withMerchantDetails() {
        ServiceEntity serviceEntity = new ServiceEntity();
        String serviceExternalId = randomUuid();
        serviceEntity.setExternalId(serviceExternalId);

        String name = "Name";
        String telephoneNumber = "03069990000";
        String addressLine1 = "Address Line 1";
        String addressLine2 = "Address Line 2";
        String addressCity = "Address City";
        String postcode = "Postcode";
        String country = "UK";
        String email = getMerchantEmail();
        MerchantDetailsEntity merchantDetailsEntity = new MerchantDetailsEntity(
                name,
                telephoneNumber,
                addressLine1,
                addressLine2,
                addressCity,
                postcode,
                country,
                email
        );
        serviceEntity.setMerchantDetailsEntity(merchantDetailsEntity);

        serviceDao.persist(serviceEntity);

        List<Map<String, Object>> savedService = databaseHelper.findServiceByExternalId(serviceExternalId);

        assertThat(savedService.size(), is(1));
        assertThat(savedService.get(0).get("external_id"), is(serviceExternalId));
        assertThat(savedService.get(0).get("merchant_name"), is(name));
        assertThat(savedService.get(0).get("merchant_telephone_number"), is(telephoneNumber));
        assertThat(savedService.get(0).get("merchant_address_line1"), is(addressLine1));
        assertThat(savedService.get(0).get("merchant_address_line2"), is(addressLine2));
        assertThat(savedService.get(0).get("merchant_address_city"), is(addressCity));
        assertThat(savedService.get(0).get("merchant_address_postcode"), is(postcode));
        assertThat(savedService.get(0).get("merchant_address_country"), is(country));
        assertThat(savedService.get(0).get("merchant_email"), is(email));
    }

    @Test
    public void shouldFindByServiceExternalId() throws Exception {
        String serviceExternalId = randomUuid();
        Service service = Service.from(randomInt(), serviceExternalId, "name");
        Map<String, Object> customBranding = ImmutableMap.of("image_url", "image url", "css_url", "css url");
        service.setCustomBranding(customBranding);
        String name = "Name";
        String telephoneNumber = "03069990000";
        String addressLine1 = "Address Line 1";
        String addressLine2 = "Address Line 2";
        String addressCity = "Address City";
        String postcode = "Postcode";
        String country = "UK";
        String email = getMerchantEmail();

        service.setMerchantDetails(new MerchantDetails(
                name,
                telephoneNumber,
                addressLine1,
                addressLine2,
                addressCity,
                postcode,
                country,
                email
        ));
        databaseHelper.addService(service, randomInt().toString());

        Optional<ServiceEntity> serviceEntity = serviceDao.findByExternalId(serviceExternalId);

        assertTrue(serviceEntity.isPresent());
        assertThat(serviceEntity.get().getId(), is(service.getId()));
        assertThat(serviceEntity.get().getName(), is("name"));

        MerchantDetailsEntity merchantDetailsEntity = serviceEntity.get().getMerchantDetailsEntity();
        assertThat(merchantDetailsEntity.getName(), is(name));
        assertThat(merchantDetailsEntity.getTelephoneNumber(), is(telephoneNumber));
        assertThat(merchantDetailsEntity.getAddressLine1(), is(addressLine1));
        assertThat(merchantDetailsEntity.getAddressLine2(), is(addressLine2));
        assertThat(merchantDetailsEntity.getAddressCity(), is(addressCity));
        assertThat(merchantDetailsEntity.getAddressPostcode(), is(postcode));
        assertThat(merchantDetailsEntity.getAddressCountryCode(), is(country));
        assertThat(merchantDetailsEntity.getEmail(), is(email));

        assertThat(serviceEntity.get().getCustomBranding().keySet().size(), is(2));
        assertThat(serviceEntity.get().getCustomBranding().keySet(), hasItems("image_url", "css_url"));
        assertThat(serviceEntity.get().getCustomBranding().values(), hasItems("image url", "css url"));
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

        assertThat(count, is(3L));
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
            String username = randomUuid();
            String email = username + "@example.com";
            UserDbFixture.userDbFixture(databaseHelper).withServiceRole(service1, roleId).withUsername(username).withEmail(email).insertUser();
        });

        //unmatching service
        String gatewayAccountId2 = randomInt().toString();
        Integer serviceId2 = randomInt();
        String externalId2 = randomUuid();
        Service service2 = Service.from(serviceId2, externalId2, Service.DEFAULT_NAME_VALUE);
        databaseHelper.addService(service2, gatewayAccountId2);

        //same user 2 diff services - should count only once
        String username3 = randomUuid();
        String email3 = username3 + "@example.com";
        User user3 = UserDbFixture.userDbFixture(databaseHelper).withServiceRole(service1, roleId).withUsername(username3).withEmail(email3).insertUser();
        databaseHelper.addUserServiceRole(user3.getId(), serviceId2, role.getId());
    }

    private String getMerchantEmail() {
        return "dd-merchant"+ randomUuid() + "@example.com";
    }
}
