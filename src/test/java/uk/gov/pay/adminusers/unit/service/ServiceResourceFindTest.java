package uk.gov.pay.adminusers.unit.service;

import io.dropwizard.testing.junit.ResourceTestRule;
import io.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.GatewayAccountIdEntity;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntityBuilder;
import uk.gov.pay.adminusers.resources.GovUkPayAgreementRequestValidator;
import uk.gov.pay.adminusers.resources.ServiceRequestValidator;
import uk.gov.pay.adminusers.resources.ServiceResource;
import uk.gov.pay.adminusers.service.GovUkPayAgreementService;
import uk.gov.pay.adminusers.service.SendLiveAccountCreatedEmailService;
import uk.gov.pay.adminusers.service.ServiceFinder;
import uk.gov.pay.adminusers.service.ServiceServicesFactory;
import uk.gov.pay.adminusers.service.StripeAgreementService;
import uk.gov.pay.adminusers.validations.RequestValidations;
import uk.gov.pay.commons.model.SupportedLanguage;

import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

@RunWith(MockitoJUnitRunner.class)
public class ServiceResourceFindTest extends ServiceResourceBaseTest {

    private static ServiceDao mockedServiceDao = mock(ServiceDao.class);
    private static UserDao mockedUserDao = mock(UserDao.class);

    private static ServiceServicesFactory mockedServicesFactory = mock(ServiceServicesFactory.class);

    private static ServiceFinder serviceFinder = new ServiceFinder(mockedServiceDao, LINKS_BUILDER);
    private static ServiceRequestValidator serviceRequestValidator = new ServiceRequestValidator(new RequestValidations(), null);
    private static StripeAgreementService stripeAgreementService = mock(StripeAgreementService.class);
    private static GovUkPayAgreementRequestValidator payAgreementRequestValidator = new GovUkPayAgreementRequestValidator(new RequestValidations());
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

    @Before
    public void setUp() {
        given(mockedServicesFactory.serviceFinder()).willReturn(serviceFinder);
    }

    @Test
    public void shouldGet_existingServiceById_withDefaultEnNameVariant() {
        String serviceExternalId = randomUuid();
        ServiceEntity serviceEntity = ServiceEntityBuilder.aServiceEntity().withExternalId(serviceExternalId).build();
        given(mockedServiceDao.findByExternalId(serviceExternalId)).willReturn(Optional.of(serviceEntity));

        Response response = RESOURCES.target(format("/v1/api/services/%s", serviceExternalId)).request().get();

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is(serviceEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName()));
        assertEnServiceNameJson(serviceEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName(), json);
        assertThat(json.getMap("service_name"), not(hasKey("cy")));
        assertMerchantDetails(serviceEntity.getMerchantDetailsEntity(), json);
        assertLinks(serviceExternalId, json);
        assertThat(json.get("redirect_to_service_immediately_on_terminal_state"), is(serviceEntity.isRedirectToServiceImmediatelyOnTerminalState()));
        assertThat(json.get("collect_billing_address"), is(serviceEntity.isCollectBillingAddress()));
        assertThat(json.get("current_go_live_stage"), is(String.valueOf(serviceEntity.getCurrentGoLiveStage())));
    }

    @Test
    public void shouldGetServiceById_withServiceNameVariantForCy() {
        String serviceExternalId = randomUuid();
        ServiceEntity serviceEntity = ServiceEntityBuilder.aServiceEntity()
                .withExternalId(serviceExternalId)
                .withServiceNameEntity(SupportedLanguage.WELSH, CY_SERVICE_NAME)
                .build();
        given(mockedServiceDao.findByExternalId(serviceExternalId)).willReturn(Optional.of(serviceEntity));
        Response response = RESOURCES.target(format("/v1/api/services/%s", serviceExternalId)).request().get();

        assertThat(response.getStatus(), is(200));
        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is(serviceEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName()));
        assertEnServiceNameJson(serviceEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName(), json);
        assertCyServiceNameJson(CY_SERVICE_NAME, json);
        assertMerchantDetails(serviceEntity.getMerchantDetailsEntity(), json);
        assertLinks(serviceExternalId, json);
    }

    @Test
    public void shouldGetServiceById_withServiceNameVariantsForEn_andCy() {
        String serviceExternalId = randomUuid();
        ServiceEntity serviceEntity = ServiceEntityBuilder.aServiceEntity()
                .withExternalId(serviceExternalId)
                .withServiceNameEntity(SupportedLanguage.ENGLISH, EN_SERVICE_NAME)
                .withServiceNameEntity(SupportedLanguage.WELSH, CY_SERVICE_NAME)
                .build();
        given(mockedServiceDao.findByExternalId(serviceExternalId)).willReturn(Optional.of(serviceEntity));
        Response response = RESOURCES.target(format("/v1/api/services/%s", serviceExternalId)).request().get();

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is(EN_SERVICE_NAME));
        assertEnServiceNameJson(EN_SERVICE_NAME, json);
        assertCyServiceNameJson(CY_SERVICE_NAME, json);
        assertMerchantDetails(serviceEntity.getMerchantDetailsEntity(), json);
        assertLinks(serviceExternalId, json);
    }

    @Test
    public void shouldFind_existingServiceByGatewayAccountId() {
        GatewayAccountIdEntity gatewayAccountIdEntity = new GatewayAccountIdEntity();
        String gatewayAccountId = randomUuid();
        gatewayAccountIdEntity.setGatewayAccountId(gatewayAccountId);
        ServiceEntity serviceEntity = ServiceEntityBuilder.aServiceEntity()
                .withGatewayAccounts(Collections.singletonList(gatewayAccountIdEntity))
                .withRedirectToServiceImmediatelyOnTerminalState(true)
                .withCreatedDate(ZonedDateTime.parse("2020-01-31T12:30:00Z"))
                .withWentLiveDate(ZonedDateTime.parse("2020-02-01T09:00:00Z"))
                .withSector("police")
                .build();
        gatewayAccountIdEntity.setService(serviceEntity);

        given(mockedServiceDao.findByGatewayAccountId(gatewayAccountId)).willReturn(Optional.of(serviceEntity));

        Response response = RESOURCES.target("/v1/api/services")
                .queryParam("gatewayAccountId", gatewayAccountId)
                .request().get();
        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is(serviceEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName()));
        assertEnServiceNameJson(serviceEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName(), json);
        assertMerchantDetails(serviceEntity.getMerchantDetailsEntity(), json);
        assertLinks(serviceEntity.getExternalId(), json);
        assertThat(json.get("redirect_to_service_immediately_on_terminal_state"), is(serviceEntity.isRedirectToServiceImmediatelyOnTerminalState()));
        assertThat(json.get("collect_billing_address"), is(serviceEntity.isCollectBillingAddress()));
        assertThat(json.get("current_go_live_stage"), is(String.valueOf(serviceEntity.getCurrentGoLiveStage())));
        assertThat(json.get("created_date"), is("2020-01-31T12:30:00.000Z"));
        assertThat(json.get("went_live_date"), is("2020-02-01T09:00:00.000Z"));
        assertThat(json.get("sector"), is("police"));
        assertThat(json.get("internal"), is(false));
        assertThat(json.get("archived"), is(false));
    }

    @Test
    public void shouldReturn404_whenFindByGatewayAccountId_ifNotFound() {
        String gatewayAccountId = randomUuid();
        given(mockedServiceDao.findByGatewayAccountId(gatewayAccountId)).willReturn(Optional.empty());

        Response response = RESOURCES.target("/v1/api/services")
                .queryParam("gatewayAccountId", gatewayAccountId)
                .request().get();
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void shouldReturn404_whenGetServiceById_ifNotFound() {
        String externalId = randomUuid();
        given(mockedServiceDao.findByExternalId(externalId)).willReturn(Optional.empty());
        Response response = RESOURCES.target(format("/v1/api/services/%s", externalId)).request().get();
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void shouldReturnBadRequest_whenGetByGatewayAccountId_isMissingQueryParam() {
        Response response = RESOURCES.target("/v1/api/services")
                .queryParam("gatewayAccountId", "")
                .request().get();
        assertThat(response.getStatus(), is(400));
        String body = response.readEntity(String.class);
        JsonPath jsonPath = JsonPath.from(body);
        assertThat(jsonPath.getList("errors"), hasSize(1));
        assertThat(jsonPath.getList("errors").get(0), is("Find services currently support only by gatewayAccountId"));
    }

    private void assertMerchantDetails(MerchantDetailsEntity merchantDetails, JsonPath jsonPath) {
        assertThat(jsonPath.get("merchant_details.address_line1"), is(merchantDetails.getAddressLine1()));
        assertThat(jsonPath.get("merchant_details.address_line2"), is(merchantDetails.getAddressLine2()));
        assertThat(jsonPath.get("merchant_details.address_country"), is(merchantDetails.getAddressCountryCode()));
        assertThat(jsonPath.get("merchant_details.address_postcode"), is(merchantDetails.getAddressPostcode()));
        assertThat(jsonPath.get("merchant_details.address_city"), is(merchantDetails.getAddressCity()));
        assertThat(jsonPath.get("merchant_details.telephone_number"), is(merchantDetails.getTelephoneNumber()));
        assertThat(jsonPath.get("merchant_details.email"), is(merchantDetails.getEmail()));
        assertThat(jsonPath.get("merchant_details.name"), is(merchantDetails.getName()));
    }
}
