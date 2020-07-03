package uk.gov.pay.adminusers.persistence.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;
import uk.gov.pay.adminusers.model.GoLiveStage;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceName;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.CustomBrandingConverter;
import uk.gov.pay.adminusers.persistence.entity.GatewayAccountIdEntity;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntityBuilder;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntityBuilder;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.commons.model.SupportedLanguage;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Comparator.comparing;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.model.Role.role;

public class ServiceDaoIT extends DaoTestBase {

    private ServiceDao serviceDao;
    private ObjectMapper objectMapper = new ObjectMapper();
    private static final String EN_NAME = "en-test-name";
    private static final String CY_NAME = "gwasanaeth prawf";

    @BeforeEach
    public void before() {
        serviceDao = env.getInstance(ServiceDao.class);
    }

    @Test
    public void shouldSaveAService_withCustomisations() throws Exception {
        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity()
                .withExperimentalFeaturesEnabled(true)
                .build();
        serviceDao.persist(thisServiceEntity);

        List<Map<String, Object>> savedService = databaseHelper.findServiceByExternalId(thisServiceEntity.getExternalId());

        assertThat(savedService.size(), is(1));
        assertThat(savedService.get(0).get("external_id"), is(thisServiceEntity.getExternalId()));
        Map<String, Object> storedBranding = objectMapper.readValue(savedService.get(0).get("custom_branding").toString(), new TypeReference<Map<String, Object>>() {
        });
        assertThat(storedBranding, is(thisServiceEntity.getCustomBranding()));
        assertThat(storedBranding.keySet().size(), is(2));
        assertThat(storedBranding.keySet(), hasItems("image_url", "css_url"));
        assertThat(storedBranding.values(), hasItems("image url", "css url"));
        assertThat(savedService.get(0).get("experimental_features_enabled"), is(true));
    }

    @Test
    public void shouldSaveAService_withMultipleServiceNames() {
        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity()
                .withCustomBranding(null)
                .withServiceNameEntity(SupportedLanguage.WELSH, CY_NAME)
                .withServiceNameEntity(SupportedLanguage.ENGLISH, EN_NAME)
                .build();
        serviceDao.persist(thisServiceEntity);

        List<Map<String, Object>> savedService = databaseHelper.findServiceByExternalId(thisServiceEntity.getExternalId());

        assertThat(savedService.size(), is(1));
        assertThat(savedService.get(0).get("external_id"), is(thisServiceEntity.getExternalId()));

        List<Map<String, Object>> savedServiceName = databaseHelper.findServiceNameByServiceId(thisServiceEntity.getId());

        assertThat(savedServiceName.size(), is(2));
        savedServiceName.sort(comparing(item -> String.valueOf(item.get("language"))));
        assertThat(savedServiceName.get(0).get("service_id"), is(Long.valueOf(thisServiceEntity.getId())));
        assertThat(savedServiceName.get(0).get("language"), is("cy"));
        assertThat(savedServiceName.get(0).get("name"), is(CY_NAME));
        assertThat(savedServiceName.get(1).get("service_id"), is(Long.valueOf(thisServiceEntity.getId())));
        assertThat(savedServiceName.get(1).get("language"), is("en"));
        assertThat(savedServiceName.get(1).get("name"), is(EN_NAME));
    }

    @Test
    public void shouldSaveAService_withoutCustomisations_andServiceName() {
        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity()
                .withCustomBranding(null)
                .withServiceNameEntity(SupportedLanguage.ENGLISH, EN_NAME)
                .withServiceNameEntity(SupportedLanguage.WELSH, CY_NAME)
                .build();
        serviceDao.persist(thisServiceEntity);

        List<Map<String, Object>> savedService = databaseHelper.findServiceByExternalId(thisServiceEntity.getExternalId());

        assertThat(savedService.size(), is(1));
        assertThat(savedService.get(0).get("external_id"), is(thisServiceEntity.getExternalId()));
        Map<String, Object> storedBranding = new CustomBrandingConverter().convertToEntityAttribute((PGobject) savedService.get(0).get("custom_branding"));
        assertNull(storedBranding);

        List<Map<String, Object>> savedServiceName = databaseHelper.findServiceNameByServiceId(thisServiceEntity.getId());
        savedServiceName.sort(comparing(item -> String.valueOf(item.get("language"))));
        assertThat(savedServiceName.size(), is(2));
        assertThat(savedServiceName.get(0).get("service_id"), is(Long.valueOf(thisServiceEntity.getId())));
        assertThat(savedServiceName.get(0).get("language"), is("cy"));
        assertThat(savedServiceName.get(0).get("name"), is(CY_NAME));
        assertThat(savedServiceName.get(1).get("service_id"), is(Long.valueOf(thisServiceEntity.getId())));
        assertThat(savedServiceName.get(1).get("language"), is("en"));
        assertThat(savedServiceName.get(1).get("name"), is(EN_NAME));
    }

    @Test
    public void shouldSaveAService_withMerchantDetails() {
        MerchantDetailsEntity merchantDetails = MerchantDetailsEntityBuilder.aMerchantDetailsEntity().build();
        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity()
                .withMerchantDetailsEntity(merchantDetails)
                .build();

        serviceDao.persist(thisServiceEntity);

        List<Map<String, Object>> savedService = databaseHelper.findServiceByExternalId(thisServiceEntity.getExternalId());

        assertThat(savedService.size(), is(1));
        assertThat(savedService.get(0).get("external_id"), is(thisServiceEntity.getExternalId()));
        assertThat(savedService.get(0).get("merchant_name"), is(merchantDetails.getName()));
        assertThat(savedService.get(0).get("merchant_telephone_number"), is(merchantDetails.getTelephoneNumber()));
        assertThat(savedService.get(0).get("merchant_address_line1"), is(merchantDetails.getAddressLine1()));
        assertThat(savedService.get(0).get("merchant_address_line2"), is(merchantDetails.getAddressLine2()));
        assertThat(savedService.get(0).get("merchant_address_city"), is(merchantDetails.getAddressCity()));
        assertThat(savedService.get(0).get("merchant_address_postcode"), is(merchantDetails.getAddressPostcode()));
        assertThat(savedService.get(0).get("merchant_address_country"), is(merchantDetails.getAddressCountryCode()));
        assertThat(savedService.get(0).get("merchant_email"), is(merchantDetails.getEmail()));
    }

    @Test
    public void shouldFindByServiceExternalId() {

        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity()
                .withExperimentalFeaturesEnabled(true)
                .build();

        databaseHelper.insertServiceEntity(thisServiceEntity);

        Optional<ServiceEntity> maybeServiceEntity = serviceDao.findByExternalId(thisServiceEntity.getExternalId());

        assertTrue(maybeServiceEntity.isPresent());
        ServiceEntity thatServiceEntity = maybeServiceEntity.get();
        assertThat(thatServiceEntity.isExperimentalFeaturesEnabled(), is(true));
        assertServiceEntity(thisServiceEntity, thatServiceEntity);

        assertMerchantDetails(thatServiceEntity.getMerchantDetailsEntity(), thisServiceEntity.getMerchantDetailsEntity());

        assertCustomBranding(thatServiceEntity);
    }

    @Test
    public void shouldFindByServiceExternalIdAndRedirectTrue() {

        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity()
                .withRedirectToServiceImmediatelyOnTerminalState(true)
                .build();

        databaseHelper.insertServiceEntity(thisServiceEntity);

        Optional<ServiceEntity> maybeServiceEntity = serviceDao.findByExternalId(thisServiceEntity.getExternalId());

        assertTrue(maybeServiceEntity.isPresent());
        ServiceEntity thatServiceEntity = maybeServiceEntity.get();
        assertServiceEntity(thisServiceEntity, thatServiceEntity);
    }

    @Test
    public void shouldFindServiceWithMultipleLanguage_byServiceExternalId() {
        Set<ServiceNameEntity> serviceNames = new HashSet<>(List.of(
                createServiceName(SupportedLanguage.ENGLISH, EN_NAME),
                createServiceName(SupportedLanguage.WELSH, CY_NAME)
        ));
        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity()
                .withServiceName(serviceNames)
                .build();

        databaseHelper.insertServiceEntity(thisServiceEntity);

        Optional<ServiceEntity> serviceEntity = serviceDao.findByExternalId(thisServiceEntity.getExternalId());
        assertTrue(serviceEntity.isPresent());
        ServiceEntity thatServiceEntity = serviceEntity.get();
        assertServiceEntity(thisServiceEntity, thatServiceEntity);

        assertMerchantDetails(thisServiceEntity.getMerchantDetailsEntity(), thatServiceEntity.getMerchantDetailsEntity());

        assertCustomBranding(thatServiceEntity);

        assertThat(thatServiceEntity.getServiceNames().size(), is(2));
        assertThat(thatServiceEntity.getServiceNames(), hasKey(SupportedLanguage.ENGLISH));
        assertThat(thatServiceEntity.getServiceNames(), hasKey(SupportedLanguage.WELSH));
    }

    @Test
    public void shouldFindByGatewayAccountId() {
        GatewayAccountIdEntity gatewayAccountIdEntity = new GatewayAccountIdEntity();
        String gatewayAccountId = randomUuid();
        gatewayAccountIdEntity.setGatewayAccountId(gatewayAccountId);
        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity()
                .withGatewayAccounts(Collections.singletonList(gatewayAccountIdEntity)).build();

        gatewayAccountIdEntity.setService(thisServiceEntity);

        databaseHelper.insertServiceEntity(thisServiceEntity);
        Optional<ServiceEntity> optionalService = serviceDao.findByGatewayAccountId(thisServiceEntity.getGatewayAccountId().getGatewayAccountId());

        assertThat(optionalService.isPresent(), is(true));
        ServiceEntity thatServiceEntity = optionalService.get();
        assertServiceEntity(thisServiceEntity, thatServiceEntity);
    }

    @Test
    public void shouldGetRoleCountForAService() {
        String serviceExternalId = randomUuid();
        Integer roleId = randomInt();
        setupUsersForServiceAndRole(serviceExternalId, roleId, 3);

        Long count = serviceDao.countOfUsersWithRoleForService(serviceExternalId, roleId);

        assertThat(count, is(3L));
    }
    
    @Test
    public void shouldMergeGoLiveStage() {
        GatewayAccountIdEntity gatewayAccountIdEntity = new GatewayAccountIdEntity();
        String gatewayAccountId = randomUuid();
        gatewayAccountIdEntity.setGatewayAccountId(gatewayAccountId);
        ServiceEntity thisServiceEntity = ServiceEntityBuilder
                .aServiceEntity()
                .withGatewayAccounts(Collections.singletonList(gatewayAccountIdEntity))
                .build();

        gatewayAccountIdEntity.setService(thisServiceEntity);

        databaseHelper.insertServiceEntity(thisServiceEntity);
        Optional<ServiceEntity> optionalService = serviceDao.findByGatewayAccountId(thisServiceEntity.getGatewayAccountId().getGatewayAccountId());
        
        assertThat(optionalService.isPresent(), is(true));
        assertThat(optionalService.get().getCurrentGoLiveStage(), is(GoLiveStage.NOT_STARTED));
        
        optionalService.get().setCurrentGoLiveStage(GoLiveStage.CHOSEN_PSP_STRIPE);
        serviceDao.merge(optionalService.get());

        optionalService = serviceDao.findByGatewayAccountId(thisServiceEntity.getGatewayAccountId().getGatewayAccountId());

        assertThat(optionalService.isPresent(), is(true));
        assertThat(optionalService.get().getCurrentGoLiveStage(), is(GoLiveStage.CHOSEN_PSP_STRIPE));
    }

    private void setupUsersForServiceAndRole(String externalId, int roleId, int noOfUsers) {
        Permission perm1 = aPermission();
        Permission perm2 = aPermission();
        databaseHelper.add(perm1).add(perm2);

        Role role = role(roleId, "role-" + roleId, "role-desc-" + roleId);
        role.setPermissions(Set.of(perm1, perm2));
        databaseHelper.add(role);

        String gatewayAccountId1 = randomInt().toString();
        Service service1 = Service.from(randomInt(), externalId, new ServiceName(Service.DEFAULT_NAME_VALUE));
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
        Service service2 = Service.from(serviceId2, externalId2, new ServiceName(Service.DEFAULT_NAME_VALUE));
        databaseHelper.addService(service2, gatewayAccountId2);

        //same user 2 diff services - should count only once
        String username3 = randomUuid();
        String email3 = username3 + "@example.com";
        User user3 = UserDbFixture.userDbFixture(databaseHelper).withServiceRole(service1, roleId).withUsername(username3).withEmail(email3).insertUser();
        databaseHelper.addUserServiceRole(user3.getId(), serviceId2, role.getId());
    }

    private static ServiceNameEntity createServiceName(SupportedLanguage language, String name) {
        ServiceNameEntity serviceNameEntity = ServiceNameEntity.from(language, name);
        serviceNameEntity.setId((long) RandomUtils.nextInt());

        return serviceNameEntity;
    }

    private void assertMerchantDetails(MerchantDetailsEntity thisEntity, MerchantDetailsEntity thatEntity) {
        assertThat(thisEntity.getName(), is(thatEntity.getName()));
        assertThat(thisEntity.getTelephoneNumber(), is(thatEntity.getTelephoneNumber()));
        assertThat(thisEntity.getAddressLine1(), is(thatEntity.getAddressLine1()));
        assertThat(thisEntity.getAddressLine2(), is(thatEntity.getAddressLine2()));
        assertThat(thisEntity.getAddressCity(), is(thatEntity.getAddressCity()));
        assertThat(thisEntity.getAddressPostcode(), is(thatEntity.getAddressPostcode()));
        assertThat(thisEntity.getAddressCountryCode(), is(thatEntity.getAddressCountryCode()));
        assertThat(thisEntity.getEmail(), is(thatEntity.getEmail()));
    }

    private void assertServiceEntity(ServiceEntity thisEntity, ServiceEntity thatEntity) {
        assertThat(thisEntity.getId(), is(thatEntity.getId()));
        assertThat(thisEntity.getExternalId(), is(thatEntity.getExternalId()));
        assertThat(thisEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName(),
                is(thatEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName()));
        assertThat(thisEntity.isRedirectToServiceImmediatelyOnTerminalState(), is(thatEntity.isRedirectToServiceImmediatelyOnTerminalState()));
        assertThat(thisEntity.isCollectBillingAddress(), is(thatEntity.isCollectBillingAddress()));
    }

    private void assertCustomBranding(ServiceEntity thisServiceEntity) {
        assertThat(thisServiceEntity.getCustomBranding().keySet().size(), is(2));
        assertThat(thisServiceEntity.getCustomBranding().keySet(), hasItems("image_url", "css_url"));
        assertThat(thisServiceEntity.getCustomBranding().values(), hasItems("image url", "css url"));
    }

}
