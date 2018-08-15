package uk.gov.pay.adminusers.unit;

import com.jayway.restassured.path.json.JsonPath;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.GatewayAccountIdEntity;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntityBuilder;
import uk.gov.pay.adminusers.persistence.entity.service.SupportedLanguage;
import uk.gov.pay.adminusers.resources.ServiceRequestValidator;
import uk.gov.pay.adminusers.resources.ServiceResource;
import uk.gov.pay.adminusers.service.LinksBuilder;
import uk.gov.pay.adminusers.service.ServiceFinder;
import uk.gov.pay.adminusers.service.ServiceServicesFactory;
import uk.gov.pay.adminusers.validations.RequestValidations;

import javax.ws.rs.core.Response;
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
public class ServiceResourceFindTest {

    private static ServiceDao mockedServiceDao = mock(ServiceDao.class);
    private static UserDao mockedUserDao = mock(UserDao.class);
    private static ServiceServicesFactory mockedServicesFactory = mock(ServiceServicesFactory.class);

    private static final String HTTPS_BASE_URL = "https://base-url";
    private static LinksBuilder linksBuilder = new LinksBuilder(HTTPS_BASE_URL);
    private static ServiceFinder serviceFinder = new ServiceFinder(mockedServiceDao, linksBuilder);
    private static ServiceRequestValidator serviceRequestValidator = new ServiceRequestValidator(new RequestValidations());

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new ServiceResource(mockedUserDao, mockedServiceDao, linksBuilder, serviceRequestValidator, mockedServicesFactory))
            .build();

    @Test
    public void shouldGet_existingServiceById_withDefaultEnNameVariant() {
        String serviceExternalId = randomUuid();
        ServiceEntity serviceEntity = ServiceEntityBuilder.aServiceEntity().withExternalId(serviceExternalId).build();
        org.mockito.BDDMockito.given(mockedServiceDao.findByExternalId(serviceExternalId)).willReturn(Optional.of(serviceEntity));

        Response response = resources.target(format("/v1/api/services/%s", serviceExternalId)).request().get();

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is(serviceEntity.getName()));
        assertEnServiceName(serviceEntity.getName(), json);
        assertThat(json.getMap("service_name"), not(hasKey("cy")));
        assertMerchantDetails(serviceEntity.getMerchantDetailsEntity(), json);
        assertLinks(serviceExternalId, json);
    }

    @Test
    public void shouldGetServiceById_withServiceNameVariantForCy() {

        String serviceExternalId = randomUuid();
        String cyName = "some-cy-name";
        ServiceEntity serviceEntity = ServiceEntityBuilder.aServiceEntity()
                .withExternalId(serviceExternalId)
                .withServiceNameEntity(SupportedLanguage.WELSH, cyName)
                .build();
        org.mockito.BDDMockito.given(mockedServiceDao.findByExternalId(serviceExternalId)).willReturn(Optional.of(serviceEntity));
        Response response = resources.target(format("/v1/api/services/%s", serviceExternalId)).request().get();

        assertThat(response.getStatus(), is(200));
        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is(serviceEntity.getName()));
        assertEnServiceName(serviceEntity.getName(), json);
        assertCyServiceName(cyName, json);
        assertMerchantDetails(serviceEntity.getMerchantDetailsEntity(), json);
        assertLinks(serviceExternalId, json);
    }

    @Test
    public void shouldGetServiceById_withServiceNameVariantsForEn_andCy() throws Exception {

        String serviceExternalId = randomUuid();
        String enName = "some-en-name";
        String cyName = "some-cy-name";
        ServiceEntity serviceEntity = ServiceEntityBuilder.aServiceEntity()
                .withName(enName)
                .withExternalId(serviceExternalId)
                .withServiceNameEntity(SupportedLanguage.ENGLISH, enName)
                .withServiceNameEntity(SupportedLanguage.WELSH, cyName)
                .build();
        org.mockito.BDDMockito.given(mockedServiceDao.findByExternalId(serviceExternalId)).willReturn(Optional.of(serviceEntity));
        Response response = resources.target(format("/v1/api/services/%s", serviceExternalId)).request().get();

        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is(serviceEntity.getName()));
        assertEnServiceName(enName, json);
        assertCyServiceName(cyName, json);
        assertMerchantDetails(serviceEntity.getMerchantDetailsEntity(), json);
        assertLinks(serviceExternalId, json);

    }

    private void assertCyServiceName(String cyName, JsonPath json) {
        assertThat(json.getMap("service_name"), hasKey("cy"));
        assertThat(json.get("service_name.cy"), is(cyName));
    }

    @Test
    public void shouldFind_existingServiceByGatewayAccountId() {

        GatewayAccountIdEntity gatewayAccountIdEntity = new GatewayAccountIdEntity();
        String gatewayAccountId = randomUuid();
        gatewayAccountIdEntity.setGatewayAccountId(gatewayAccountId);
        ServiceEntity serviceEntity = ServiceEntityBuilder.aServiceEntity()
                .withGatewayAccounts(Collections.singletonList(gatewayAccountIdEntity))
                .build();
        gatewayAccountIdEntity.setService(serviceEntity);

        given(mockedServicesFactory.serviceFinder()).willReturn(serviceFinder);
        given(mockedServiceDao.findByGatewayAccountId(gatewayAccountId)).willReturn(Optional.of(serviceEntity));

        Response response = resources.target("/v1/api/services")
                .queryParam("gatewayAccountId", gatewayAccountId)
                .request().get();
        assertThat(response.getStatus(), is(200));

        String body = response.readEntity(String.class);
        JsonPath json = JsonPath.from(body);

        assertThat(json.get("name"), is(serviceEntity.getName()));
        assertEnServiceName(serviceEntity.getName(), json);
        assertMerchantDetails(serviceEntity.getMerchantDetailsEntity(), json);
        assertLinks(serviceEntity.getExternalId(), json);
    }

    @Test
    public void shouldReturn404_whenFindByGatewayAccountId_ifNotFound() {
        String gatewayAccountId = randomUuid();
        given(mockedServicesFactory.serviceFinder()).willReturn(serviceFinder);
        given(mockedServiceDao.findByGatewayAccountId(gatewayAccountId)).willReturn(Optional.empty());

        Response response = resources.target("/v1/api/services")
                .queryParam("gatewayAccountId", gatewayAccountId)
                .request().get();
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void shouldReturn404_whenGetServiceById_ifNotFound() {
        String externalId = randomUuid();
        org.mockito.BDDMockito.given(mockedServiceDao.findByExternalId(externalId)).willReturn(Optional.empty());
        Response response = resources.target(format("/v1/api/services/%s", externalId)).request().get();
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void shouldReturnBadRequest_whenGetByGatewayAccountId_isMissingQueryParam() {
        Response response = resources.target("/v1/api/services")
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

    private void assertLinks(String serviceExternalId, JsonPath json) {
        assertThat(json.getList("_links"), hasSize(1));
        assertThat(json.get("_links[0].href"), is(HTTPS_BASE_URL + "/v1/api/services/" + serviceExternalId));
        assertThat(json.get("_links[0].method"), is("GET"));
        assertThat(json.get("_links[0].rel"), is("self"));
    }

    private void assertEnServiceName(String name, JsonPath json) {
        assertThat(json.getMap("service_name"), hasKey("en"));
        assertThat(json.get("service_name.en"), is(name));
    }
}
