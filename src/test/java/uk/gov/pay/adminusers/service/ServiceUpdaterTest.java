package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;

public class ServiceUpdaterTest {

    ServiceDao serviceDao = mock(ServiceDao.class);
    ServiceUpdater updater;

    @Before
    public void before() throws Exception {
        updater = new ServiceUpdater(serviceDao);
    }

    @Test
    public void shouldUpdateNameSuccessfully() throws Exception {

        String serviceId = randomUuid();
        ServiceUpdateRequest request = mock(ServiceUpdateRequest.class);
        ServiceEntity serviceEntity = mock(ServiceEntity.class);
        String nameToUpdate = "new-name";

        when(request.getPath()).thenReturn("name");
        when(request.getValue()).thenReturn(singletonList(nameToUpdate));
        when(serviceDao.findByExternalId(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(serviceEntity.toService()).thenReturn(Service.from());

        updater.doUpdate(serviceId, request);

        verify(serviceEntity,times(1)).setName(nameToUpdate);
        verify(serviceDao, times(1)).merge(serviceEntity);
    }

    @Test
    public void shouldUpdateCustomBrandingSuccessfully() throws Exception {

        String serviceId = randomUuid();
        ServiceUpdateRequest request = mock(ServiceUpdateRequest.class);
        ServiceEntity serviceEntity = mock(ServiceEntity.class);
        String customBranding = "custom branding";

        when(request.getPath()).thenReturn("custom_branding");
        when(request.getValue()).thenReturn(singletonList(customBranding));
        when(serviceDao.findByExternalId(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(serviceEntity.toService()).thenReturn(Service.from());

        updater.doUpdate(serviceId, request);

        verify(serviceEntity,times(1)).setCustomBranding(customBranding);
        verify(serviceDao, times(1)).merge(serviceEntity);
    }

    @Test
    public void shouldSuccess_whenAddGatewayAccountToService() throws Exception {
        String serviceId = randomUuid();
        ServiceUpdateRequest request = mock(ServiceUpdateRequest.class);
        ServiceEntity serviceEntity = mock(ServiceEntity.class);
        List<String> gatewayAccountIdsToUpdate = asList("1", "2");

        when(request.getPath()).thenReturn("gateway_account_ids");
        when(request.getValue()).thenReturn(gatewayAccountIdsToUpdate);
        when(serviceDao.findByExternalId(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(serviceDao.checkIfGatewayAccountsUsed(gatewayAccountIdsToUpdate)).thenReturn(false);
        when(serviceEntity.toService()).thenReturn(Service.from());

        updater.doUpdate(serviceId, request);

        verify(serviceEntity,times(1)).addGatewayAccountIds(gatewayAccountIdsToUpdate.toArray(new String[0]));
        verify(serviceDao, times(1)).merge(serviceEntity);
    }


    @Test(expected = WebApplicationException.class)
    public void shouldError_IfAGatewayAccountAlreadyAssignedToAService() throws Exception {
        String serviceId = randomUuid();
        ServiceUpdateRequest request = mock(ServiceUpdateRequest.class);
        ServiceEntity serviceEntity = mock(ServiceEntity.class);
        List<String> gatewayAccountIdsToUpdate = asList("1", "2");

        when(request.getPath()).thenReturn("gateway_account_ids");
        when(request.getValue()).thenReturn(gatewayAccountIdsToUpdate);
        when(serviceDao.findByExternalId(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(serviceDao.checkIfGatewayAccountsUsed(gatewayAccountIdsToUpdate)).thenReturn(true);

        updater.doUpdate(serviceId, request);

        verify(serviceEntity,times(0)).addGatewayAccountIds(gatewayAccountIdsToUpdate.toArray(new String[0]));
        verify(serviceDao, times(0)).merge(serviceEntity);
    }
}
