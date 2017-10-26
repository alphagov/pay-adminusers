package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.adminusers.model.UpdateMerchantDetailsRequest;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
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
        String nameToUpdate = "new-name";
        ServiceUpdateRequest request = ServiceUpdateRequest.from(new ObjectNode(JsonNodeFactory.instance, ImmutableMap.of(
                "path", new TextNode("name"),
                "value", new TextNode(nameToUpdate),
                "op", new TextNode("replace"))));
        ServiceEntity serviceEntity = mock(ServiceEntity.class);

        when(serviceDao.findByExternalId(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(serviceEntity.toService()).thenReturn(Service.from());

        updater.doUpdate(serviceId, request);

        verify(serviceEntity, times(1)).setName(nameToUpdate);
        verify(serviceDao, times(1)).merge(serviceEntity);
    }

    @Test
    public void shouldUpdateMerchantDetailsSuccessfully() throws Exception {
        String serviceId = randomUuid();
        String name = "name";
        String addressLine1 = "something";
        String addressLine2 = "something";
        String addressCity = "something";
        String addressPostcode = "something";
        String addressCountry = "something";

        MerchantDetailsEntity toUpdate = new MerchantDetailsEntity(name, addressLine1, addressLine2, addressCity, addressPostcode, addressCountry);
        UpdateMerchantDetailsRequest request = new UpdateMerchantDetailsRequest(name, addressLine1, addressLine2, addressCity, addressPostcode, addressCountry);
        ServiceEntity serviceEntity = mock(ServiceEntity.class);

        when(serviceDao.findByExternalId(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(serviceEntity.toService()).thenReturn(Service.from());

        updater.doUpdateMerchantDetails(serviceId, request);

        verify(serviceEntity, times(1)).setMerchantDetailsEntity(toUpdate);
        verify(serviceDao, times(1)).merge(serviceEntity);
    }

    @Test
    public void shouldSuccess_updateCustomBranding_whenBrandingProvided() throws Exception {
        String serviceId = randomUuid();
        ServiceUpdateRequest request = mock(ServiceUpdateRequest.class);
        ServiceEntity serviceEntity = mock(ServiceEntity.class);
        Map<String, Object> customBranding = ImmutableMap.of("image_url", "image url", "css_url", "css url");

        when(request.getPath()).thenReturn("custom_branding");
        when(request.valueAsObject()).thenReturn(customBranding);
        when(serviceDao.findByExternalId(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(serviceEntity.toService()).thenReturn(Service.from());

        updater.doUpdate(serviceId, request);

        verify(serviceEntity, times(1)).setCustomBranding(customBranding);
        verify(serviceDao, times(1)).merge(serviceEntity);
    }

    @Test
    public void shouldSuccess_updateCustomBranding_whenBrandingNotProvided() throws Exception {
        String serviceId = randomUuid();
        ServiceUpdateRequest request = mock(ServiceUpdateRequest.class);
        ServiceEntity serviceEntity = mock(ServiceEntity.class);

        when(request.getPath()).thenReturn("custom_branding");
        when(request.valueAsObject()).thenReturn(null);
        when(serviceDao.findByExternalId(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(serviceEntity.toService()).thenReturn(Service.from());

        updater.doUpdate(serviceId, request);

        verify(serviceEntity, times(1)).setCustomBranding(null);
        verify(serviceDao, times(1)).merge(serviceEntity);
    }

    @Test
    public void shouldSuccess_whenAddGatewayAccountToService() throws Exception {
        String serviceId = randomUuid();
        ServiceUpdateRequest request = mock(ServiceUpdateRequest.class);
        ServiceEntity serviceEntity = mock(ServiceEntity.class);
        List<String> gatewayAccountIdsToUpdate = asList("1", "2");

        when(request.getPath()).thenReturn("gateway_account_ids");
        when(request.valueAsList()).thenReturn(gatewayAccountIdsToUpdate);
        when(serviceDao.findByExternalId(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(serviceDao.checkIfGatewayAccountsUsed(gatewayAccountIdsToUpdate)).thenReturn(false);
        when(serviceEntity.toService()).thenReturn(Service.from());

        updater.doUpdate(serviceId, request);

        verify(serviceEntity, times(1)).addGatewayAccountIds(gatewayAccountIdsToUpdate.toArray(new String[0]));
        verify(serviceDao, times(1)).merge(serviceEntity);
    }

    @Test(expected = WebApplicationException.class)
    public void shouldError_IfAGatewayAccountAlreadyAssignedToAService() throws Exception {
        String serviceId = randomUuid();
        ServiceUpdateRequest request = mock(ServiceUpdateRequest.class);
        ServiceEntity serviceEntity = mock(ServiceEntity.class);
        List<String> gatewayAccountIdsToUpdate = asList("1", "2");

        when(request.getPath()).thenReturn("gateway_account_ids");
        when(request.valueAsList()).thenReturn(gatewayAccountIdsToUpdate);
        when(serviceDao.findByExternalId(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(serviceDao.checkIfGatewayAccountsUsed(gatewayAccountIdsToUpdate)).thenReturn(true);

        updater.doUpdate(serviceId, request);

        verify(serviceEntity, times(0)).addGatewayAccountIds(gatewayAccountIdsToUpdate.toArray(new String[0]));
        verify(serviceDao, times(0)).merge(serviceEntity);
    }

}
