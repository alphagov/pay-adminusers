package uk.gov.pay.adminusers.unit.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.fixtures.ServiceEntityFixture;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntityBuilder;
import uk.gov.pay.adminusers.resources.GovUkPayAgreementRequestValidator;
import uk.gov.pay.adminusers.resources.ServiceRequestValidator;
import uk.gov.pay.adminusers.resources.ServiceResource;
import uk.gov.pay.adminusers.service.*;
import uk.gov.pay.adminusers.validations.RequestValidations;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.pay.adminusers.JsonResourceLoader.load;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.resources.ServiceRequestValidator.SERVICE_SEARCH_LENGTH_ERR_MSG;
import static uk.gov.pay.adminusers.resources.ServiceRequestValidator.SERVICE_SEARCH_SPECIAL_CHARS_ERR_MSG;

@ExtendWith(MockitoExtension.class)
@ExtendWith(DropwizardExtensionsSupport.class)
public class ServiceResourceSearchTest extends ServiceResourceBaseTest {

    // mocks
    private static final ServiceDao mockServiceDao = mock(ServiceDao.class);
    private static final UserDao mockUserDao = mock(UserDao.class);
    private static final StripeAgreementService mockStripeAgreementService = mock(StripeAgreementService.class);
    private static final GovUkPayAgreementService mockAgreementService = mock(GovUkPayAgreementService.class);
    private static final SendLiveAccountCreatedEmailService mockSendLiveAccountCreatedEmailService = mock(SendLiveAccountCreatedEmailService.class);
    private static final ServiceServicesFactory mockedServicesFactory = mock(ServiceServicesFactory.class);
    private static final GovUkPayAgreementRequestValidator mockPayAgreementRequestValidator = mock(GovUkPayAgreementRequestValidator.class);
    // --

    private static final ServiceRequestValidator serviceRequestValidator = new ServiceRequestValidator(new RequestValidations(), null);
    private static final ServiceFinder serviceFinder = new ServiceFinder(mockedServiceDao, LINKS_BUILDER);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final ResourceExtension underTest = ResourceExtension.builder()
            .addResource(new ServiceResource(
                    mockUserDao,
                    mockServiceDao,
                    LINKS_BUILDER,
                    serviceRequestValidator,
                    mockedServicesFactory,
                    mockStripeAgreementService,
                    mockPayAgreementRequestValidator,
                    mockAgreementService,
                    mockSendLiveAccountCreatedEmailService))
            .build();

    @BeforeEach
    public void setUp() {
        given(mockedServicesFactory.serviceFinder()).willReturn(serviceFinder);
    }

    @Test
    public void shouldOK_andReturnServices_whenMatches() throws Exception {
        var payload = load("fixtures/resource/service/post/service-search-request.json");
        var serviceExternalId = randomUuid();
        var merchantDetailsEntity = MerchantDetailsEntityBuilder.aMerchantDetailsEntity()
                .withName("Government Bakery Office")
                .build();
        var serviceEntity = ServiceEntityFixture.aServiceEntity()
                .withExternalId(serviceExternalId)
                .withMerchantDetailsEntity(merchantDetailsEntity)
                .withServiceNameEntity(SupportedLanguage.ENGLISH, "GOV.UK Cake Service")
                .build();

        given(mockedServiceDao.findByENServiceName("cake")).willReturn(List.of(serviceEntity));
        given(mockedServiceDao.findByServiceMerchantName("bakery")).willReturn(List.of(serviceEntity));

        var response = underTest.target("/v1/api/services/search").request().post(Entity.json(payload));

        assertThat(response.getStatus(), is(200));
        var json = mapper.readTree(response.readEntity(String.class));
        assertThat(json.get("name_results").size(), is(1));
        assertThat(json.get("merchant_results").size(), is(1));
        assertThat(json.get("name_results").get(0).get("external_id").asText(), is(serviceExternalId));
        assertThat(json.get("name_results").get(0).get("name").asText(), is("GOV.UK Cake Service"));
        assertThat(json.get("merchant_results").get(0).get("external_id").asText(), is(serviceExternalId));
        assertThat(json.get("merchant_results").get(0).get("merchant_details").get("name").asText(), is("Government Bakery Office"));
    }

    @Test
    public void shouldOK_andReturnEmptyResult_whenNoMatches() throws Exception {
        var payload = load("fixtures/resource/service/post/service-search-request.json");

        given(mockedServiceDao.findByENServiceName("cake")).willReturn(Collections.emptyList());
        given(mockedServiceDao.findByServiceMerchantName("bakery")).willReturn(Collections.emptyList());

        var response = underTest.target("/v1/api/services/search").request().post(Entity.json(payload));

        assertThat(response.getStatus(), is(200));
        var json = mapper.readTree(response.readEntity(String.class));
        assertThat(json.get("name_results").size(), is(0));
        assertThat(json.get("merchant_results").size(), is(0));
    }

    @Test
    public void shouldError_whenRequestValidationFails() throws JsonProcessingException {
        var payload = "{\"service_name\": \"!@£$%^\", \"service_merchant_name\": \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";

        Response response = underTest.target("/v1/api/services/search").request().post(Entity.json(payload));

        assertThat(response.getStatus(), is(400));
        JsonNode json = mapper.readTree(response.readEntity(String.class));
        assertThat(json.get("errors").size(), is(2));
        var errors = List.of(json.get("errors").get(0).asText(), json.get("errors").get(1).asText());
        errors.forEach(err -> assertThat(err, anyOf(equalTo(SERVICE_SEARCH_LENGTH_ERR_MSG), equalTo(SERVICE_SEARCH_SPECIAL_CHARS_ERR_MSG))));
        verify(mockServiceDao, never()).findByServiceMerchantName(anyString());
        verify(mockServiceDao, never()).findByENServiceName(anyString());
    }

}
