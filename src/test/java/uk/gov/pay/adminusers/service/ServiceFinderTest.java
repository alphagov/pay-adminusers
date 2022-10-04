package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceSearchRequest;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntityBuilder;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

@ExtendWith(MockitoExtension.class)
public class ServiceFinderTest {

    @Mock
    private ServiceDao serviceDao;

    private ServiceFinder serviceFinder;

    @BeforeEach
    public void before() {
        serviceFinder = new ServiceFinder(serviceDao, new LinksBuilder("http://localhost"));
    }

    @Test
    public void shouldReturnService_ifFoundByGatewayAccountId() {
        String gatewayAccountId = "1";
        ServiceEntity serviceEntity = new ServiceEntity();
        serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(SupportedLanguage.ENGLISH, Service.DEFAULT_NAME_VALUE));
        serviceEntity.addGatewayAccountIds(gatewayAccountId);
        when(serviceDao.findByGatewayAccountId(gatewayAccountId)).thenReturn(Optional.of(serviceEntity));

        Optional<Service> serviceOptional = serviceFinder.byGatewayAccountId(gatewayAccountId);

        assertThat(serviceOptional.isPresent(), is(true));
        assertThat(serviceOptional.get().getGatewayAccountIds().get(0), is(gatewayAccountId));
    }

    @Test
    public void shouldReturnService_ifFoundByExternalId() {
        String gatewayAccountId = "1";
        String externalId = randomUuid();
        ServiceEntity serviceEntity = new ServiceEntity();
        serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(SupportedLanguage.ENGLISH, Service.DEFAULT_NAME_VALUE));
        serviceEntity.addGatewayAccountIds(gatewayAccountId);
        serviceEntity.setExternalId(externalId);
        when(serviceDao.findByExternalId(externalId)).thenReturn(Optional.of(serviceEntity));

        Optional<Service> serviceOptional = serviceFinder.byExternalId(externalId);

        assertThat(serviceOptional.isPresent(), is(true));
        assertThat(serviceOptional.get().getGatewayAccountIds().get(0), is(gatewayAccountId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnServices_whenSearchingByServiceName() {
        var serviceEntities = generateServiceEntities("serv 1", "serv 2");
        when(serviceDao.findByENServiceName("serv")).thenReturn(serviceEntities);
        var searchRequest = new ServiceSearchRequest("serv", "");
        var results = serviceFinder.bySearchRequest(searchRequest);
        var servicesByName = (List<Service>) results.getNameResults();
        var servicesByMerchant = (List<Service>) serviceFinder.bySearchRequest(searchRequest).getMerchantResults();
        assertThat(servicesByName.size(), is(2));
        assertThat(servicesByName.stream().map(Service::getName).collect(Collectors.toSet()), containsInAnyOrder("serv 1", "serv 2"));
        assertThat(servicesByMerchant, is(empty()));
        verify(serviceDao, never()).findByServiceMerchantName(anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnServices_whenSearchingByServiceMerchantName() {
        var serviceEntities = generateServiceEntities("serv 3", "serv 4");
        when(serviceDao.findByServiceMerchantName("merchant name")).thenReturn(serviceEntities);
        var searchRequest = new ServiceSearchRequest("", "merchant name");
        var results = serviceFinder.bySearchRequest(searchRequest);
        var servicesByName = (List<Service>) results.getNameResults();
        var servicesByMerchant = (List<Service>) serviceFinder.bySearchRequest(searchRequest).getMerchantResults();
        assertThat(servicesByMerchant.size(), is(2));
        assertThat(servicesByMerchant.stream().map(Service::getName).collect(Collectors.toSet()), containsInAnyOrder("serv 3", "serv 4"));
        assertThat(servicesByName, is(empty()));
        verify(serviceDao, never()).findByENServiceName(anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnServices_whenSearchingByServiceNameAndMerchantName() {
        var serviceEntities1 = generateServiceEntities("serv 1", "serv 2");
        var serviceEntities2 = generateServiceEntities("serv 3", "serv 4");
        when(serviceDao.findByENServiceName("serv")).thenReturn(serviceEntities1);
        when(serviceDao.findByServiceMerchantName("merchant name")).thenReturn(serviceEntities2);
        var searchRequest = new ServiceSearchRequest("serv", "merchant name");
        var results = serviceFinder.bySearchRequest(searchRequest);
        var servicesByName = (List<Service>) results.getNameResults();
        var servicesByMerchant = (List<Service>) serviceFinder.bySearchRequest(searchRequest).getMerchantResults();
        assertThat(servicesByName.size(), is(2));
        assertThat(servicesByMerchant.size(), is(2));
        assertThat(servicesByName.stream().map(Service::getName).collect(Collectors.toSet()), containsInAnyOrder("serv 1", "serv 2"));
        assertThat(servicesByMerchant.stream().map(Service::getName).collect(Collectors.toSet()), containsInAnyOrder("serv 3", "serv 4"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnEmptyLists_whenSearchRequestParamsAreBlank() {
        var searchRequest = new ServiceSearchRequest("", "");
        var results = serviceFinder.bySearchRequest(searchRequest);
        var servicesByName = (List<Service>) results.getNameResults();
        var servicesByMerchant = (List<Service>) serviceFinder.bySearchRequest(searchRequest).getMerchantResults();
        verify(serviceDao, never()).findByENServiceName(anyString());
        verify(serviceDao, never()).findByServiceMerchantName(anyString());
        assertThat(servicesByName, is(empty()));
        assertThat(servicesByMerchant, is(empty()));
    }

    @Test
    public void shouldReturnEmpty_ifNotFound() {
        String gatewayAccountId = "1";
        when(serviceDao.findByGatewayAccountId(gatewayAccountId)).thenReturn(Optional.empty());
        Optional<Service> serviceOptional = serviceFinder.byGatewayAccountId(gatewayAccountId);

        assertThat(serviceOptional.isPresent(), is(false));
    }

    private static List<ServiceEntity> generateServiceEntities(String... names) {
        var serviceEntities = new ArrayList<ServiceEntity>();
        for (String name : names) {
            serviceEntities.add(ServiceEntityBuilder.aServiceEntity()
                    .withServiceNameEntity(SupportedLanguage.ENGLISH, name)
                    .build());
        }
        return serviceEntities;
    }

}
