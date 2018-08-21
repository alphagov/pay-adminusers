package uk.gov.pay.adminusers.unit.service;

import com.jayway.restassured.path.json.JsonPath;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.adminusers.persistence.entity.service.SupportedLanguage;
import uk.gov.pay.adminusers.resources.ServiceRequestValidator;
import uk.gov.pay.adminusers.resources.ServiceResource;
import uk.gov.pay.adminusers.service.ServiceCreator;
import uk.gov.pay.adminusers.service.ServiceServicesFactory;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static uk.gov.pay.adminusers.resources.ServiceRequestValidator.FIELD_GATEWAY_ACCOUNT_IDS;
import static uk.gov.pay.adminusers.resources.ServiceRequestValidator.FIELD_NAME;
import static uk.gov.pay.adminusers.resources.ServiceResource.SERVICES_RESOURCE;

@RunWith(MockitoJUnitRunner.class)
public class ServiceResourceCreateTest extends ServiceResourceBaseTest {

    private static final Map<String, Object> PAYLOAD_MAP = new HashMap<>();

    private static UserDao mockedUserDao = mock(UserDao.class);
    private static ServiceServicesFactory mockedServicesFactory = mock(ServiceServicesFactory.class);

    private static ServiceCreator serviceCreator = new ServiceCreator(mockedServiceDao, linksBuilder);
    private static ServiceCreator mockedServiceCreator = mock(ServiceCreator.class);
    private static ServiceRequestValidator serviceRequestValidator = new ServiceRequestValidator(new RequestValidations());

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new ServiceResource(mockedUserDao, mockedServiceDao, linksBuilder, serviceRequestValidator, mockedServicesFactory))
            .build();

    @Captor
    private ArgumentCaptor<ServiceEntity> serviceEntityArgumentCaptor;

    @Before
    public void setUp() {
        given(mockedServicesFactory.serviceCreator()).willReturn(mockedServiceCreator);
    }

    @After
    public void tearDown() {
        Mockito.reset(mockedServiceDao);
        Mockito.reset(mockedServiceCreator);
        PAYLOAD_MAP.clear();
    }

    @Test
    public void shouldSuccess_whenCreateAServiceWithoutParameters() {
        Service service = buildService(Optional.empty(), Optional.empty(), Collections.emptyMap());
        given(mockedServiceCreator.doCreate(Optional.empty(), Optional.empty(), Collections.emptyMap()))
                .willReturn(service);
        Response response = resources.target(SERVICES_RESOURCE)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(PAYLOAD_MAP), Response.class);

        assertThat(response.getStatus(), is(201));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is("System Generated"));
        assertThat(json.get("external_id"), is(notNullValue()));
        assertThat(json.getMap("service_name"), not(hasKey("cy")));
        assertEnServiceNameJson("System Generated", json);
        assertLinks(json.get("external_id"), json);
    }

    @Test
    public void shouldSuccess_whenCreateAServiceWithNameOnly() {

        PAYLOAD_MAP.put(FIELD_NAME, EN_SERVICE_NAME);

        Service service = buildService(Optional.of(EN_SERVICE_NAME), Optional.empty(), Collections.emptyMap());
        given(mockedServiceCreator.doCreate(Optional.of(EN_SERVICE_NAME), Optional.empty(), Collections.emptyMap()))
                .willReturn(service);
        Response response = resources.target(SERVICES_RESOURCE)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(PAYLOAD_MAP), Response.class);

        assertThat(response.getStatus(), is(201));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("external_id"), is(notNullValue()));
        assertThat(json.get("name"), is(EN_SERVICE_NAME));
        assertEnServiceNameJson(EN_SERVICE_NAME, json);
        assertThat(json.getMap("service_name"), not(hasKey("cy")));
        assertLinks(json.get("external_id"), json);
    }

    @Test
    public void shouldSuccess_whenCreateAServiceWithName_andGatewayAccountIds() {

        PAYLOAD_MAP.put(FIELD_NAME, EN_SERVICE_NAME);
        String anotherGatewayAccountId = "another-gateway-account-id";
        List<String> gatewayAccounts = Arrays.asList(GATEWAY_ACCOUNT_ID, anotherGatewayAccountId);
        PAYLOAD_MAP.put(FIELD_GATEWAY_ACCOUNT_IDS, gatewayAccounts);

        Service service = buildService(Optional.of(EN_SERVICE_NAME), Optional.of(gatewayAccounts), Collections.emptyMap());
        given(mockedServiceCreator.doCreate(Optional.of(EN_SERVICE_NAME), Optional.of(gatewayAccounts), Collections.emptyMap()))
                .willReturn(service);

        Response response = resources.target(SERVICES_RESOURCE)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(PAYLOAD_MAP), Response.class);
        assertThat(response.getStatus(), is(201));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is(EN_SERVICE_NAME));
        assertThat(json.get("external_id"), is(notNullValue()));
        assertEnServiceNameJson(EN_SERVICE_NAME, json);
        assertThat(json.getMap("service_name"), not(hasKey("cy")));
        assertLinks(json.get("external_id"), json);
    }

    @Test
    public void shouldSuccess_whenCreateAServiceWithName_andGatewayAccountIds_andServiceNameVariants_englishAndCymru() {

        PAYLOAD_MAP.put(FIELD_NAME, EN_SERVICE_NAME);
        String anotherGatewayAccountId = "another-gateway-account-id";
        List<String> gatewayAccounts = Arrays.asList(GATEWAY_ACCOUNT_ID, anotherGatewayAccountId);
        PAYLOAD_MAP.put(FIELD_GATEWAY_ACCOUNT_IDS, gatewayAccounts);
        Map<String, String> nameVariants = new HashMap<>();
        nameVariants.put("en", EN_SERVICE_NAME);
        nameVariants.put("cy", CY_SERVICE_NAME);
        PAYLOAD_MAP.put("service_name", nameVariants);

        Map<SupportedLanguage, String> serviceName = new HashMap<>();
        serviceName.put(SupportedLanguage.ENGLISH, EN_SERVICE_NAME);
        serviceName.put(SupportedLanguage.WELSH, CY_SERVICE_NAME);

        Service service = buildService(Optional.of(EN_SERVICE_NAME), Optional.of(gatewayAccounts), serviceName);
        given(mockedServiceCreator.doCreate(Optional.of(EN_SERVICE_NAME), Optional.of(gatewayAccounts), serviceName))
                .willReturn(service);

        Response response = resources.target(SERVICES_RESOURCE)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(PAYLOAD_MAP), Response.class);

        assertThat(response.getStatus(), is(201));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is(EN_SERVICE_NAME));
        assertThat(json.get("external_id"), is(notNullValue()));
        assertEnServiceNameJson(EN_SERVICE_NAME, json);
        assertCyServiceNameJson(CY_SERVICE_NAME, json);
        assertLinks(json.get("external_id"), json);
        assertThat(json.getList("gateway_account_ids"), hasSize(2));
        assertThat(json.getList("gateway_account_ids"), containsInAnyOrder(GATEWAY_ACCOUNT_ID, anotherGatewayAccountId));
    }

    @Test
    public void shouldError409_whenGatewayAccountsAreAlreadyAssignedToAService() {
        PAYLOAD_MAP.put(FIELD_NAME, EN_SERVICE_NAME);
        PAYLOAD_MAP.put(FIELD_GATEWAY_ACCOUNT_IDS, Collections.singletonList(GATEWAY_ACCOUNT_ID));

        given(mockedServicesFactory.serviceCreator()).willReturn(serviceCreator);
        given(mockedServiceDao.checkIfGatewayAccountsUsed(anyListOf(String.class))).willReturn(true);
        Response response = resources.target(SERVICES_RESOURCE)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(PAYLOAD_MAP), Response.class);
        assertThat(response.getStatus(), is(409));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);
        assertThat(json.getList("errors"), hasSize(1));
        assertThat(json.getList("errors"), containsInAnyOrder("One or more of the following gateway account ids has already assigned to another service: [some-gateway-account-id]"));
        Mockito.verify(mockedServiceDao, never()).persist(serviceEntityArgumentCaptor.capture());
    }

    private Service buildService(Optional<String> maybeName,
                                 Optional<List<String>> maybeAccountIds,
                                 Map<SupportedLanguage, String> serviceNameVariants) {
        Service service = maybeName.map(Service::from)
                .orElseGet(Service::from);
        ServiceEntity serviceEntity = ServiceEntity.from(service);
        serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(SupportedLanguage.ENGLISH, service.getName()));
        serviceNameVariants.forEach((k, v) -> serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(k, v)));
        if (maybeAccountIds.isPresent()) {
            List<String> gatewayAccountsIds = maybeAccountIds.get();
            serviceEntity.addGatewayAccountIds(gatewayAccountsIds.toArray(new String[0]));
        }
        return linksBuilder.decorate(serviceEntity.toService());
    }
}
