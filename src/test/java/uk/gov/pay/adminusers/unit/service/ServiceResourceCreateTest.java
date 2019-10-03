package uk.gov.pay.adminusers.unit.service;

import io.dropwizard.testing.junit.ResourceTestRule;
import io.restassured.path.json.JsonPath;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.adminusers.resources.GovUkPayAgreementRequestValidator;
import uk.gov.pay.adminusers.resources.ServiceRequestValidator;
import uk.gov.pay.adminusers.resources.ServiceResource;
import uk.gov.pay.adminusers.resources.ServiceUpdateOperationValidator;
import uk.gov.pay.adminusers.service.GovUkPayAgreementService;
import uk.gov.pay.adminusers.service.SendLiveAccountCreatedEmailService;
import uk.gov.pay.adminusers.service.ServiceCreator;
import uk.gov.pay.adminusers.service.ServiceServicesFactory;
import uk.gov.pay.adminusers.service.StripeAgreementService;
import uk.gov.pay.adminusers.validations.RequestValidations;
import uk.gov.pay.commons.model.SupportedLanguage;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static uk.gov.pay.adminusers.resources.ServiceResource.FIELD_NAME;
import static uk.gov.pay.adminusers.resources.ServiceResource.SERVICES_RESOURCE;
import static uk.gov.pay.adminusers.service.ServiceUpdater.FIELD_GATEWAY_ACCOUNT_IDS;

@RunWith(MockitoJUnitRunner.class)
public class ServiceResourceCreateTest extends ServiceResourceBaseTest {

    private static final Map<String, Object> PAYLOAD_MAP = new HashMap<>();

    private static UserDao mockedUserDao = mock(UserDao.class);
    private static ServiceServicesFactory mockedServicesFactory = mock(ServiceServicesFactory.class);

    private static ServiceCreator serviceCreator = new ServiceCreator(mockedServiceDao, LINKS_BUILDER);
    private static ServiceCreator mockedServiceCreator = mock(ServiceCreator.class);

    private static RequestValidations requestValidations = new RequestValidations();
    private static ServiceRequestValidator serviceRequestValidator = new ServiceRequestValidator(requestValidations, new ServiceUpdateOperationValidator(requestValidations));
    private static StripeAgreementService stripeAgreementService = mock(StripeAgreementService.class);
    private static GovUkPayAgreementRequestValidator payAgreementRequestValidator = new GovUkPayAgreementRequestValidator(requestValidations);
    private static GovUkPayAgreementService agreementService = mock(GovUkPayAgreementService.class);
    private static SendLiveAccountCreatedEmailService sendLiveAccountCreatedEmailService = mock(SendLiveAccountCreatedEmailService.class);

    @ClassRule
    public static final ResourceTestRule RESOURCES = ResourceTestRule.builder()
            .addResource(new ServiceResource(
                    mockedUserDao,
                    mockedServiceDao,
                    LINKS_BUILDER,
                    serviceRequestValidator,
                    mockedServicesFactory,
                    stripeAgreementService,
                    payAgreementRequestValidator,
                    agreementService,
                    sendLiveAccountCreatedEmailService))
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
        Service service = buildService(Collections.emptyList(), Collections.emptyMap());
        given(mockedServiceCreator.doCreate(Collections.emptyList(), Collections.emptyMap()))
                .willReturn(service);
        Response response = RESOURCES.target(SERVICES_RESOURCE)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(PAYLOAD_MAP), Response.class);

        assertThat(response.getStatus(), is(201));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is("System Generated"));
        assertThat(json.get("external_id"), is(notNullValue()));
        assertThat(json.get("redirect_to_service_immediately_on_terminal_state"), is(false));
        assertThat(json.get("collect_billing_address"), is(true));
        assertThat(json.getMap("service_name"), not(hasKey("cy")));
        assertEnServiceNameJson("System Generated", json);
        assertLinks(json.get("external_id"), json);
    }

    @Test
    public void shouldSuccess_whenCreateAServiceWithNameOnly() {
        PAYLOAD_MAP.put("service_name", Map.of(SupportedLanguage.ENGLISH.toString(), EN_SERVICE_NAME));

        Service service = buildService(Collections.emptyList(), Map.of(SupportedLanguage.ENGLISH, EN_SERVICE_NAME));
        given(mockedServiceCreator.doCreate(Collections.emptyList(), Map.of(SupportedLanguage.ENGLISH, EN_SERVICE_NAME)))
                .willReturn(service);
        Response response = RESOURCES.target(SERVICES_RESOURCE)
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
    public void shouldSuccess_whenCreateAServiceWithEnglishNameOnly() {
        PAYLOAD_MAP.put("service_name", Map.of(SupportedLanguage.ENGLISH.toString(), EN_SERVICE_NAME));

        Service service = buildService(Collections.emptyList(), Map.of(SupportedLanguage.ENGLISH, EN_SERVICE_NAME));
        given(mockedServiceCreator.doCreate(Collections.emptyList(), Map.of(SupportedLanguage.ENGLISH, EN_SERVICE_NAME)))
                .willReturn(service);
        Response response = RESOURCES.target(SERVICES_RESOURCE)
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
        PAYLOAD_MAP.put("service_name", Map.of(SupportedLanguage.ENGLISH.toString(), EN_SERVICE_NAME));
        String anotherGatewayAccountId = "another-gateway-account-id";
        List<String> gatewayAccounts = Arrays.asList(GATEWAY_ACCOUNT_ID, anotherGatewayAccountId);
        PAYLOAD_MAP.put(FIELD_GATEWAY_ACCOUNT_IDS, gatewayAccounts);

        Service service = buildService(gatewayAccounts, Map.of(SupportedLanguage.ENGLISH, EN_SERVICE_NAME));
        given(mockedServiceCreator.doCreate(gatewayAccounts, Map.of(SupportedLanguage.ENGLISH, EN_SERVICE_NAME)))
                .willReturn(service);

        Response response = RESOURCES.target(SERVICES_RESOURCE)
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
    public void shouldSuccess_whenCreateAServiceWithEnglishName_andGatewayAccountIds() {
        PAYLOAD_MAP.put("service_name", Map.of(SupportedLanguage.ENGLISH.toString(), EN_SERVICE_NAME));
        String anotherGatewayAccountId = "another-gateway-account-id";
        List<String> gatewayAccounts = List.of(GATEWAY_ACCOUNT_ID, anotherGatewayAccountId);
        PAYLOAD_MAP.put(FIELD_GATEWAY_ACCOUNT_IDS, gatewayAccounts);

        Service service = buildService(gatewayAccounts, Map.of(SupportedLanguage.ENGLISH, EN_SERVICE_NAME));
        given(mockedServiceCreator.doCreate(gatewayAccounts, Map.of(SupportedLanguage.ENGLISH, EN_SERVICE_NAME)))
                .willReturn(service);

        Response response = RESOURCES.target(SERVICES_RESOURCE)
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
        List<String> gatewayAccounts = List.of(GATEWAY_ACCOUNT_ID, anotherGatewayAccountId);
        PAYLOAD_MAP.put(FIELD_GATEWAY_ACCOUNT_IDS, gatewayAccounts);
        PAYLOAD_MAP.put("service_name",
                Map.of(SupportedLanguage.ENGLISH.toString(), EN_SERVICE_NAME, SupportedLanguage.WELSH.toString(), CY_SERVICE_NAME));

        Map<SupportedLanguage, String> serviceName = Map.of(SupportedLanguage.ENGLISH, EN_SERVICE_NAME, SupportedLanguage.WELSH, CY_SERVICE_NAME);

        Service service = buildService(gatewayAccounts, serviceName);
        given(mockedServiceCreator.doCreate(gatewayAccounts, serviceName))
                .willReturn(service);

        Response response = RESOURCES.target(SERVICES_RESOURCE)
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
        given(mockedServiceDao.checkIfGatewayAccountsUsed(anyList())).willReturn(true);
        Response response = RESOURCES.target(SERVICES_RESOURCE)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(PAYLOAD_MAP), Response.class);
        assertThat(response.getStatus(), is(409));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);
        assertThat(json.getList("errors"), hasSize(1));
        assertThat(json.getList("errors"), containsInAnyOrder("One or more of the following gateway account ids has already assigned to another service: [some-gateway-account-id]"));
        Mockito.verify(mockedServiceDao, never()).persist(serviceEntityArgumentCaptor.capture());
    }

    private Service buildService(List<String> gatewayAccountIds, Map<SupportedLanguage, String> serviceNames) {
        ServiceEntity serviceEntity = ServiceEntity.from(Service.from());
        serviceNames.forEach((language, name) -> serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(language, name)));
        serviceEntity.addGatewayAccountIds(gatewayAccountIds.toArray(new String[0]));
        return LINKS_BUILDER.decorate(serviceEntity.toService());
    }
}
