package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class ServiceCreatorTest {

    @Mock
    ServiceDao serviceDao;

    @Captor
    ArgumentCaptor<ServiceEntity> persistedServiceEntity;

    ServiceCreator serviceCreator;

    @Before
    public void before() throws Exception {
        serviceCreator = new ServiceCreator(serviceDao, new LinksBuilder("http://localhost"));
    }

    @Test
    public void shouldSuccess_whenProvidedNoNameOrGatewayIds() throws Exception {
        Service service = serviceCreator.doCreate(Optional.empty(), Optional.empty());

        verify(serviceDao, times(1)).persist(persistedServiceEntity.capture());
        assertThat(service.getName(), is("System Generated"));
        List<String> persistedGatewayIds = persistedServiceEntity.getValue().getGatewayAccountIds().stream().map(gai -> gai.getGatewayAccountId()).collect(toList());
        assertThat(persistedGatewayIds.size(),is(0));
    }

    @Test
    public void shouldSuccess_whenProvidedOnlyAValidName() throws Exception {
        Service service = serviceCreator.doCreate(Optional.of("blah blah"), Optional.empty());

        verify(serviceDao, times(1)).persist(persistedServiceEntity.capture());
        assertThat(service.getName(), is("blah blah"));

        List<String> persistedGatewayIds = persistedServiceEntity.getValue().getGatewayAccountIds().stream().map(gai -> gai.getGatewayAccountId()).collect(toList());
        assertThat(persistedGatewayIds.size(),is(0));
    }

    @Test
    public void shouldSuccess_whenProvidedOnlyWithUnassignedGatewayID() throws Exception {

        Service service = serviceCreator.doCreate(Optional.empty(), Optional.of(asList("1", "2")));

        verify(serviceDao, times(1)).persist(persistedServiceEntity.capture());

        assertThat(service.getName(), is("System Generated"));
        List<String> persistedGatewayIds = persistedServiceEntity.getValue().getGatewayAccountIds().stream().map(gai -> gai.getGatewayAccountId()).collect(toList());
        assertThat(persistedGatewayIds.size(),is(2));
        assertThat(persistedGatewayIds,hasItems("1","2"));
    }

    @Test
    public void shouldSuccess_whenProvidedBothAValidNameAndAnUnassignedGatewayID() throws Exception {
        Service service = serviceCreator.doCreate(Optional.of("blah blah"), Optional.of(asList("1", "2")));

        verify(serviceDao, times(1)).persist(persistedServiceEntity.capture());

        assertThat(service.getName(), is("blah blah"));
        List<String> persistedGatewayIds = persistedServiceEntity.getValue().getGatewayAccountIds().stream().map(gai -> gai.getGatewayAccountId()).collect(toList());
        assertThat(persistedGatewayIds.size(),is(2));
        assertThat(persistedGatewayIds,hasItems("1","2"));

    }

    @Test(expected = WebApplicationException.class)
    public void shouldFail_whenProvidedAConflictingGatewayID() throws Exception {
        List<String> gatewayAccountsIds = asList("3");
        when(serviceDao.checkIfGatewayAccountsUsed(gatewayAccountsIds)).thenReturn(true);
        serviceCreator.doCreate(Optional.of("blah blah"), Optional.of(gatewayAccountsIds));
    }

}