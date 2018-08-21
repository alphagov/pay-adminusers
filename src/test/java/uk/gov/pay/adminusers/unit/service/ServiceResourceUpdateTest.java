package uk.gov.pay.adminusers.unit.service;

import com.jayway.restassured.path.json.JsonPath;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.GatewayAccountIdEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntityBuilder;
import uk.gov.pay.adminusers.persistence.entity.service.SupportedLanguage;
import uk.gov.pay.adminusers.resources.ServiceRequestValidator;
import uk.gov.pay.adminusers.resources.ServiceResource;
import uk.gov.pay.adminusers.service.ServiceServicesFactory;
import uk.gov.pay.adminusers.service.ServiceUpdater;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Optional;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class ServiceResourceUpdateTest extends ServiceResourceBaseTest {

    private static final String API_PATH = "/v1/api/services/%s";
    private static UserDao mockedUserDao = mock(UserDao.class);
    private static ServiceServicesFactory mockedServicesFactory = mock(ServiceServicesFactory.class);

    private static ServiceUpdater serviceUpdater = new ServiceUpdater(mockedServiceDao);
    private static ServiceRequestValidator requestValidator = new ServiceRequestValidator(new RequestValidations());

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new ServiceResource(
                    mockedUserDao,
                    mockedServiceDao,
                    linksBuilder,
                    requestValidator,
                    mockedServicesFactory))
            .build();

    @Before
    public void setUp() {
        when(mockedServicesFactory.serviceUpdater()).thenReturn(serviceUpdater);
    }

    @Test
    public void shouldSuccess_whenUpdateOnlyServiceName() {

        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity().build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = fixture("fixtures/resource/service/patch/update-name-only.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = resources.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is("new-en-name"));
    }

    @Test
    public void shouldSuccess_whenReplaceServiceNameAnd_addCyName() {

        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity().build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = fixture("fixtures/resource/service/patch/update-name-add-cy-name.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = resources.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is(thisServiceEntity.getName()));
        assertCyServiceNameJson("new-cy-name", json);
    }

    @Test
    public void shouldSuccess_whenReplaceServiceNameAnd_replaceCyName() {

        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity()
                .withName("old-en-name")
                .withServiceNameEntity(SupportedLanguage.WELSH, "old-cy-name")
                .build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = fixture("fixtures/resource/service/patch/update-name-and-update-cy-name.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = resources.target(format(API_PATH, thisServiceEntity.getExternalId()))
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
    public void shouldSuccess_whenUpdateOnlyCyName() {

        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity()
                .withServiceNameEntity(SupportedLanguage.WELSH, "old-cy-name")
                .build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = fixture("fixtures/resource/service/patch/update-cy-name-only.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = resources.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is("System Generated"));
        assertCyServiceNameJson("new-cy-name", json);
    }

    @Test
    public void shouldError404_ifServiceExternalIdDoesNotExist() {
        String jsonPayload = fixture("fixtures/resource/service/patch/update-cy-name-only.json");
        String externalId = "externalId";
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.empty());

        Response response = resources.target(format(API_PATH, externalId))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void shouldError400_ifFieldValueMissing() {

        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity().build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = fixture("fixtures/resource/service/patch/update-name-missing-value.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = resources.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(400));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.getList("errors"), hasSize(1));
        assertThat(json.getList("errors"), containsInAnyOrder("Field [value] is required"));
    }

    @Test
    public void shouldError400_ifFieldPathMissing() {

        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity().build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = fixture("fixtures/resource/service/patch/update-name-missing-path.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = resources.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(400));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.getList("errors"), hasSize(1));
        assertThat(json.getList("errors"), containsInAnyOrder("Field [path] is required"));
    }

    @Test
    public void shouldError400_ifFieldOpMissing() {

        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity().build();
        String externalId = thisServiceEntity.getExternalId();

        String jsonPayload = fixture("fixtures/resource/service/patch/update-name-missing-op.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);

        Response response = resources.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(400));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.getList("errors"), hasSize(1));
        assertThat(json.getList("errors"), containsInAnyOrder("Field [op] is required"));
    }

    @Test
    public void shouldSuccess_whenAddGatewayAccountIds_whereNoGatewayAccountIds() {
        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity().build();
        String externalId = thisServiceEntity.getExternalId();

        assertThat(thisServiceEntity.getGatewayAccountIds(), is(empty()));

        String jsonPayload = fixture("fixtures/resource/service/patch/update-gateway-account-id.json");
        when(mockedServiceDao.findByExternalId(externalId)).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);
        when(mockedServiceDao.checkIfGatewayAccountsUsed(Collections.singletonList("1014748185"))).thenReturn(false);

        Response response = resources.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.getList("gateway_account_ids"), contains("1014748185"));
    }

    @Test
    public void shouldSuccess_whenAddGatewayAccountIds_whereThereIsGatewayAccountIds() {
        GatewayAccountIdEntity gatewayAccountIdEntity = new GatewayAccountIdEntity();
        String gatewayAccountId = randomUuid();
        gatewayAccountIdEntity.setGatewayAccountId(gatewayAccountId);
        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity()
                .withGatewayAccounts(Collections.singletonList(gatewayAccountIdEntity))
                .build();
        gatewayAccountIdEntity.setService(thisServiceEntity);

        String jsonPayload = fixture("fixtures/resource/service/patch/update-gateway-account-id.json");
        when(mockedServiceDao.findByExternalId(thisServiceEntity.getExternalId())).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);
        when(mockedServiceDao.checkIfGatewayAccountsUsed(Collections.singletonList("1014748185"))).thenReturn(false);

        Response response = resources.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.getList("gateway_account_ids"), containsInAnyOrder("1014748185", gatewayAccountId));
    }

    @Test
    public void shouldReturn409_whenAddGatewayAccountIds_andGatewayAccountId_isUsedByAnotherService() {
        ServiceEntity thisServiceEntity = ServiceEntityBuilder.aServiceEntity().build();

        String jsonPayload = fixture("fixtures/resource/service/patch/update-gateway-account-id.json");
        when(mockedServiceDao.findByExternalId(thisServiceEntity.getExternalId())).thenReturn(Optional.of(thisServiceEntity));
        when(mockedServiceDao.merge(thisServiceEntity)).thenReturn(thisServiceEntity);
        when(mockedServiceDao.checkIfGatewayAccountsUsed(Collections.singletonList("1014748185"))).thenReturn(true);

        Response response = resources.target(format(API_PATH, thisServiceEntity.getExternalId()))
                .request()
                .method("PATCH", Entity.json(jsonPayload));

        assertThat(response.getStatus(), is(409));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.getList("errors"), hasSize(1));
        assertThat(json.getList("errors"), contains("One or more of the following gateway account ids has already assigned to another service: [1014748185]"));
    }

}
