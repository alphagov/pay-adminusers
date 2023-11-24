package uk.gov.pay.adminusers.unit.service;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.adminusers.expungeandarchive.service.ExpungeAndArchiveHistoricalDataService;
import uk.gov.pay.adminusers.fixtures.ServiceEntityFixture;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.GatewayAccountIdEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.resources.GovUkPayAgreementRequestValidator;
import uk.gov.pay.adminusers.resources.ServiceRequestValidator;
import uk.gov.pay.adminusers.resources.ServiceResource;
import uk.gov.pay.adminusers.resources.ServiceUpdateOperationValidator;
import uk.gov.pay.adminusers.service.GovUkPayAgreementService;
import uk.gov.pay.adminusers.service.SendLiveAccountCreatedEmailService;
import uk.gov.pay.adminusers.service.ServiceServicesFactory;
import uk.gov.pay.adminusers.service.ServiceUpdater;
import uk.gov.pay.adminusers.service.StripeAgreementService;
import uk.gov.pay.adminusers.validations.RequestValidations;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Optional;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.JsonResourceLoader.load;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

@ExtendWith(DropwizardExtensionsSupport.class)
public class ServiceResourceUpdateTest extends ServiceResourceBaseTest {

    private static final String API_PATH = "/v1/api/services/%s";
    private static UserDao mockedUserDao = mock(UserDao.class);
    private static ServiceServicesFactory mockedServicesFactory = mock(ServiceServicesFactory.class);

    private static ExpungeAndArchiveHistoricalDataService expungeAndArchiveHistoricalDataService = mock(ExpungeAndArchiveHistoricalDataService.class);
    private static ServiceUpdater serviceUpdater = new ServiceUpdater(mockedServiceDao, expungeAndArchiveHistoricalDataService);
    private static RequestValidations requestValidations = new RequestValidations();
    private static ServiceRequestValidator requestValidator = new ServiceRequestValidator(requestValidations, new ServiceUpdateOperationValidator(requestValidations));
    private static StripeAgreementService stripeAgreementService = mock(StripeAgreementService.class);
    private static GovUkPayAgreementRequestValidator payAgreementRequestValidator = new GovUkPayAgreementRequestValidator(requestValidations);
    private static GovUkPayAgreementService agreementService = mock(GovUkPayAgreementService.class);
    private static SendLiveAccountCreatedEmailService sendLiveAccountCreatedEmailService = mock(SendLiveAccountCreatedEmailService.class);

    public static final ResourceExtension RESOURCES = ResourceExtension.builder()
            .addResource(new ServiceResource(
                    mockedUserDao,
                    mockedServiceDao,
                    LINKS_BUILDER,
                    requestValidator,
                    mockedServicesFactory,
                    stripeAgreementService,
                    payAgreementRequestValidator,
                    agreementService,
                    sendLiveAccountCreatedEmailService))
            .build();

    @BeforeEach
    public void setUp() {
        when(mockedServicesFactory.serviceUpdater()).thenReturn(serviceUpdater);
    }

    @Test
    public void shouldUpdateExistingEnServiceNameIncludingLegacyName_inSingleObject() throws Exception {

        ServiceEntity thisServiceEntity = ServiceEntityFixture.aServiceEntity().build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/single-object-replace-service-name-en.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is("new-en-name"));
        assertEnServiceNameJson("new-en-name", json);
    }

    @Test
    public void shouldUpdateExistingEnServiceNameIncludingLegacyName() throws Exception {

        ServiceEntity thisServiceEntity = ServiceEntityFixture.aServiceEntity().build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/array-replace-service-name-en.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is("new-en-name"));
        assertEnServiceNameJson("new-en-name", json);
    }

    @Test
    public void shouldUpdateExistingEnServiceNameAndNonExistingCyServiceName() throws Exception {

        ServiceEntity thisServiceEntity = ServiceEntityFixture.aServiceEntity().build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/array-replace-service-name-en-replace-service-name-cy.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is("new-en-name"));
        assertEnServiceNameJson("new-en-name", json);
        assertCyServiceNameJson("new-cy-name", json);
    }

    @Test
    public void shouldUpdateExistingCyServiceName() throws Exception {

        ServiceEntity thisServiceEntity = ServiceEntityFixture.aServiceEntity()
                .withServiceNameEntity(SupportedLanguage.WELSH, "old-cy-name")
                .build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/array-replace-service-name-cy.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is("System Generated"));
        assertEnServiceNameJson("System Generated", json);
        assertCyServiceNameJson("new-cy-name", json);
    }

    @Test
    public void shouldUpdateExistingEnServiceNameAndExistingCyServiceName() throws Exception {

        ServiceEntity thisServiceEntity = ServiceEntityFixture.aServiceEntity()
                .withServiceNameEntity(SupportedLanguage.ENGLISH, "old-en-name")
                .withServiceNameEntity(SupportedLanguage.WELSH, "old-cy-name")
                .build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/array-replace-service-name-en-replace-service-name-cy.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is("new-en-name"));
        assertEnServiceNameJson("new-en-name", json);
        assertCyServiceNameJson("new-cy-name", json);
    }

    @Test
    public void shouldRemoveCyServiceNameWhenReplacedWithBlank() throws Exception {

        ServiceEntity thisServiceEntity = ServiceEntityFixture.aServiceEntity()
                .withServiceNameEntity(SupportedLanguage.ENGLISH, "old-en-name")
                .withServiceNameEntity(SupportedLanguage.WELSH, "old-cy-name")
                .build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/array-replace-service-name-cy-blank.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is("old-en-name"));
        assertEnServiceNameJson("old-en-name", json);
        assertThat(json.getMap("service_name"), not(hasKey(SupportedLanguage.WELSH.toString())));
    }

    @Test
    public void shouldError404_ifServiceExternalIdDoesNotExist() throws Exception {
        String jsonPayload = load("fixtures/resource/service/patch/array-replace-service-name-en.json");
        String externalId = "externalId";
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.empty());

        Response response = RESOURCES.target(format(API_PATH, externalId))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void shouldError400_ifMandatoryFieldValueMissing() throws Exception {

        ServiceEntity thisServiceEntity = ServiceEntityFixture.aServiceEntity().build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/array-replace-service-name-en-missing-value.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(400));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.getList("errors"), hasSize(1));
        assertThat(json.getList("errors"), containsInAnyOrder("Field [value] is required"));
    }

    @Test
    public void shouldError400_ifMandatoryFieldPathMissing() throws Exception {

        ServiceEntity thisServiceEntity = ServiceEntityFixture.aServiceEntity().build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/array-replace-missing-path.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(400));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.getList("errors"), hasSize(1));
        assertThat(json.getList("errors"), containsInAnyOrder("Field [path] is required"));
    }

    @Test
    public void shouldError400_ifmandatoryFieldOpMissing() throws Exception {

        ServiceEntity thisServiceEntity = ServiceEntityFixture.aServiceEntity().build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/array-missing-op-service-name-en.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(400));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.getList("errors"), hasSize(1));
        assertThat(json.getList("errors"), containsInAnyOrder("Field [op] is required"));
    }

    @Test
    public void shouldSuccess_whenAddGatewayAccountIds_whereNoGatewayAccountIds() throws Exception {
        ServiceEntity thisServiceEntity = ServiceEntityFixture.aServiceEntity().build();
        String externalId = thisServiceEntity.getExternalId();

        assertThat(thisServiceEntity.getGatewayAccountIds(), is(empty()));

        String jsonPayload = load("fixtures/resource/service/patch/array-add-gateway-account-ids.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);
        when(mockedServiceDao.checkIfGatewayAccountsUsed(Collections.singletonList("1014748185"))).thenReturn(false);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.getList("gateway_account_ids"), contains("1014748185"));
    }

    @Test
    public void shouldSuccess_whenAddGatewayAccountIds_whereThereIsGatewayAccountIds() throws Exception {
        GatewayAccountIdEntity gatewayAccountIdEntity = new GatewayAccountIdEntity();
        String gatewayAccountId = randomUuid();
        gatewayAccountIdEntity.setGatewayAccountId(gatewayAccountId);
        ServiceEntity thisServiceEntity = ServiceEntityFixture.aServiceEntity()
                .withGatewayAccounts(Collections.singletonList(gatewayAccountIdEntity))
                .build();
        gatewayAccountIdEntity.setService(thisServiceEntity);

        String jsonPayload = load("fixtures/resource/service/patch/array-add-gateway-account-ids.json");
        when(mockedServiceDao.findByExternalId(thisServiceEntity.getExternalId())).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);
        when(mockedServiceDao.checkIfGatewayAccountsUsed(Collections.singletonList("1014748185"))).thenReturn(false);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.getList("gateway_account_ids"), containsInAnyOrder("1014748185", gatewayAccountId));
    }

    @Test
    public void shouldReturn409_whenAddGatewayAccountIds_andGatewayAccountId_isUsedByAnotherService() throws Exception {
        ServiceEntity thisServiceEntity = ServiceEntityFixture.aServiceEntity().build();

        String jsonPayload = load("fixtures/resource/service/patch/array-add-gateway-account-ids.json");
        when(mockedServiceDao.findByExternalId(thisServiceEntity.getExternalId())).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);
        when(mockedServiceDao.checkIfGatewayAccountsUsed(Collections.singletonList("1014748185"))).thenReturn(true);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(409));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.getList("errors"), hasSize(1));
        assertThat(json.getList("errors"), contains("One or more of the following gateway account ids has already assigned to another service: [1014748185]"));
    }

    @Test
    public void shouldUpdateRedirect_toTrue() throws Exception {
        ServiceEntity thisServiceEntity = ServiceEntityFixture
                .aServiceEntity()
                .withRedirectToServiceImmediatelyOnTerminalState(false)
                .build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/replace_redirect_immediately_to_true.json");

        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("redirect_to_service_immediately_on_terminal_state"), is(true));
    }

    @Test
    public void shouldFailUpdateRedirect_whenValueIsNotBoolean() throws Exception {
        ServiceEntity thisServiceEntity = ServiceEntityFixture
                .aServiceEntity()
                .withRedirectToServiceImmediatelyOnTerminalState(false)
                .build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/replace_redirect_immediately_invalid_value.json");

        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(400));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("errors"), is(Collections.singletonList("Field [value] must be a boolean")));
    }

    @Test
    public void shouldUpdateCollectBillingAddress_toFalse() throws Exception {
        ServiceEntity thisServiceEntity = ServiceEntityFixture
                .aServiceEntity()
                .withCollectBillingAddress(true)
                .build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/replace_collect_billing_address_to_false.json");

        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("collect_billing_address"), is(false));
    }

    @Test
    public void shouldFailUpdateCollectBillingAddress_whenValueIsNotBoolean() throws Exception {
        ServiceEntity thisServiceEntity = ServiceEntityFixture
                .aServiceEntity()
                .withCollectBillingAddress(true)
                .build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/replace_collect_billing_address_invalid_value.json");

        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(400));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("errors"), is(Collections.singletonList("Field [value] must be a boolean")));
    }

    @Test
    public void shouldUpdateExperimentalFeaturesEnabled_toTrue() throws Exception {
        ServiceEntity thisServiceEntity = ServiceEntityFixture
                .aServiceEntity()
                .withExperimentalFeaturesEnabled(false)
                .build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/replace_experimental_features_enabled_to_true.json");

        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("experimental_features_enabled"), is(true));
    }

    @Test
    public void shouldUpdateTakesPaymentsOverPhone_toTrue() throws Exception {
        ServiceEntity thisServiceEntity = ServiceEntityFixture
                .aServiceEntity()
                .withTakesPaymentsOverPhone(false)
                .build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = load("fixtures/resource/service/patch/replace_takes_payments_over_phone_to_true.json");

        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = RESOURCES.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("takes_payments_over_phone"), is(true));
    }
}
