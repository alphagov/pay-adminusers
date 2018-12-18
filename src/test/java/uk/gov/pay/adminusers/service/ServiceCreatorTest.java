package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.Link;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.GatewayAccountIdEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.commons.model.SupportedLanguage;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceCreatorTest {

    private static final String EN_SERVICE_NAME = "en-service-name";
    private static final String CY_SERVICE_NAME = "cy-service-name";
    private static final String BASE_URL = "http://localhost";

    @Mock
    ServiceDao mockedServiceDao;

    @Captor
    ArgumentCaptor<ServiceEntity> persistedServiceEntity;
    @Captor
    private ArgumentCaptor<List<String>> listArgumentCaptor;

    private ServiceCreator serviceCreator;

    @Before
    public void before() {
        serviceCreator = new ServiceCreator(mockedServiceDao, new LinksBuilder(BASE_URL));
    }

    @Test
    public void shouldSuccess_whenProvidedWith_noParameters() {
        Service service = serviceCreator.doCreate(Optional.empty(), Optional.empty(), Collections.emptyMap());

        verify(mockedServiceDao, never()).checkIfGatewayAccountsUsed(anyList());
        verify(mockedServiceDao, times(1)).persist(persistedServiceEntity.capture());
        assertThat(service.getName(), is("System Generated"));
        assertThat(service.isRedirectToServiceImmediatelyOnTerminalState(), is(false));
        assertThat(service.isCollectBillingAddress(), is(true));
        List<GatewayAccountIdEntity> persistedGatewayIds = persistedServiceEntity.getValue().getGatewayAccountIds();
        assertThat(persistedGatewayIds.size(), is(0));
        assertEnServiceNameMap(service, "System Generated");
        assertSelfLink(service);
    }

    @Test
    public void shouldSuccess_whenProvidedWith_onlyAValidName() {
        Service service = serviceCreator.doCreate(Optional.of(EN_SERVICE_NAME), Optional.empty(), Collections.emptyMap());

        verify(mockedServiceDao, never()).checkIfGatewayAccountsUsed(anyList());
        verify(mockedServiceDao, times(1)).persist(persistedServiceEntity.capture());
        assertThat(service.getName(), is(EN_SERVICE_NAME));

        List<GatewayAccountIdEntity> persistedGatewayIds = persistedServiceEntity.getValue().getGatewayAccountIds();
        assertThat(persistedGatewayIds.size(), is(0));
        assertEnServiceNameMap(service, EN_SERVICE_NAME);
        assertSelfLink(service);
    }

    @Test
    public void shouldSuccess_whenProvidedWith_multipleValidNames_andNoGatewayAccountIds() {
        Map<SupportedLanguage, String> serviceNameVariants = new HashMap<>();
        serviceNameVariants.put(SupportedLanguage.WELSH, CY_SERVICE_NAME);
        Service service = serviceCreator.doCreate(Optional.of(EN_SERVICE_NAME), Optional.empty(), serviceNameVariants);

        verify(mockedServiceDao, never()).checkIfGatewayAccountsUsed(anyList());
        verify(mockedServiceDao, times(1)).persist(persistedServiceEntity.capture());
        assertThat(service.getName(), is(EN_SERVICE_NAME));

        List<GatewayAccountIdEntity> persistedGatewayIds = persistedServiceEntity.getValue().getGatewayAccountIds();
        assertThat(persistedGatewayIds.size(), is(0));
        assertEnServiceNameMap(service, EN_SERVICE_NAME);
        assertThat(service.getServiceNames(), hasKey(SupportedLanguage.WELSH.toString()));
        assertThat(service.getServiceNames(), hasValue(CY_SERVICE_NAME));
        assertSelfLink(service);
    }

    @Test
    public void shouldSuccess_whenProvidedWith_unassignedGatewayId() {
        String gatewayAccountId_2 = "gatewayAccountId_2";
        String gatewayAccountId_1 = "gatewayAccountId_1";
        Service service = serviceCreator.doCreate(Optional.empty(), Optional.of(asList(gatewayAccountId_1, gatewayAccountId_2)), Collections.emptyMap());

        verify(mockedServiceDao, times(1)).checkIfGatewayAccountsUsed(anyList());
        verify(mockedServiceDao, times(1)).persist(persistedServiceEntity.capture());

        assertThat(service.getName(), is("System Generated"));

        List<String> persistedGatewayIds = persistedServiceEntity.getValue().getGatewayAccountIds().stream().map(GatewayAccountIdEntity::getGatewayAccountId).collect(toList());
        assertThat(persistedGatewayIds.size(), is(2));
        assertThat(persistedGatewayIds, hasItems(gatewayAccountId_1, gatewayAccountId_2));

        assertThat(service.getGatewayAccountIds(), hasItems(gatewayAccountId_1, gatewayAccountId_2));
        assertEnServiceNameMap(service, "System Generated");
        assertSelfLink(service);

        verify(mockedServiceDao).checkIfGatewayAccountsUsed(listArgumentCaptor.capture());
        List<String> gatewayAccounts = listArgumentCaptor.getValue();
        assertThat(gatewayAccounts.size(), is(2));
        assertThat(gatewayAccounts, containsInAnyOrder(gatewayAccountId_1, gatewayAccountId_2));
    }

    @Test
    public void shouldSuccess_whenProvidedWith_validName_AndUnassignedGatewayId() {
        String gatewayAccountId_2 = "gatewayAccountId_2";
        String gatewayAccountId_1 = "gatewayAccountId_1";
        Service service = serviceCreator.doCreate(Optional.of(EN_SERVICE_NAME), Optional.of(asList(gatewayAccountId_1, gatewayAccountId_2)), Collections.emptyMap());

        verify(mockedServiceDao, times(1)).checkIfGatewayAccountsUsed(anyList());
        verify(mockedServiceDao, times(1)).persist(persistedServiceEntity.capture());

        assertThat(service.getName(), is(EN_SERVICE_NAME));
        List<String> persistedGatewayIds = persistedServiceEntity.getValue().getGatewayAccountIds().stream().map(GatewayAccountIdEntity::getGatewayAccountId).collect(toList());
        assertThat(persistedGatewayIds.size(), is(2));
        assertThat(persistedGatewayIds, hasItems(gatewayAccountId_1, gatewayAccountId_1));
        assertThat(service.getGatewayAccountIds(), hasItems(gatewayAccountId_1, gatewayAccountId_2));

        assertEnServiceNameMap(service, EN_SERVICE_NAME);

        assertSelfLink(service);
    }

    @Test(expected = WebApplicationException.class)
    public void shouldFail_whenProvidedAConflictingGatewayID() {
        List<String> gatewayAccountsIds = Collections.singletonList("3");
        when(mockedServiceDao.checkIfGatewayAccountsUsed(gatewayAccountsIds)).thenReturn(true);
        serviceCreator.doCreate(Optional.of(EN_SERVICE_NAME), Optional.of(gatewayAccountsIds), Collections.emptyMap());
    }

    private void assertEnServiceNameMap(Service service, String serviceName) {
        assertThat(service.getServiceNames(), hasKey(SupportedLanguage.ENGLISH.toString()));
        assertThat(service.getServiceNames(), hasValue(serviceName));
    }

    private void assertSelfLink(Service service) {
        assertThat(service.getLinks(), hasSize(1));
        Link selfLink = service.getLinks().get(0);
        assertThat(selfLink.getRel(), is(Link.Rel.self));
        assertThat(selfLink.getMethod(), is(HttpMethod.GET));
        assertThat(selfLink.getHref(), is(BASE_URL + "/v1/api/services/" + service.getExternalId()));
    }
}
