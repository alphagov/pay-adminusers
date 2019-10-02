package uk.gov.pay.adminusers.service;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.commons.model.SupportedLanguage;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceFinderTest {

    @Mock
    private ServiceDao serviceDao;

    private ServiceFinder serviceFinder;

    @Before
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
    public void shouldReturnEmpty_ifNotFound() {
        String gatewayAccountId = "1";
        when(serviceDao.findByGatewayAccountId(gatewayAccountId)).thenReturn(Optional.empty());
        Optional<Service> serviceOptional = serviceFinder.byGatewayAccountId(gatewayAccountId);

        assertThat(serviceOptional.isPresent(), is(false));
    }

}
