package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import uk.gov.pay.adminusers.exception.ServiceNotFoundException;
import uk.gov.pay.adminusers.model.GoLiveStage;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceUpdateRequest;
import uk.gov.pay.adminusers.model.UpdateMerchantDetailsRequest;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.commons.model.SupportedLanguage;

import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntityBuilder.aMerchantDetailsEntity;
import static uk.gov.pay.adminusers.persistence.entity.ServiceEntityBuilder.aServiceEntity;

public class ServiceUpdaterTest {

    private static final String NON_EXISTENT_SERVICE_EXTERNAL_ID = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    public static final String SERVICE_ID = randomUuid();
    private ServiceDao serviceDao = mock(ServiceDao.class);
    private ServiceUpdater updater;

    @Before
    public void before() {
        updater = new ServiceUpdater(serviceDao);
    }

    @Test
    public void shouldUpdateNameSuccessfully() {
        String nameToUpdate = "new-name";
        ServiceUpdateRequest request = serviceUpdateRequest("replace", "name", nameToUpdate);
        ServiceEntity serviceEntity = new ServiceEntity();

        when(serviceDao.findByExternalId(SERVICE_ID)).thenReturn(of(serviceEntity));

        Optional<Service> maybeService = updater.doUpdate(SERVICE_ID, request);

        assertThat(maybeService.isPresent(), is(true));
        assertThat(maybeService.get().getServiceNames().size(), is(1));
        assertThat(maybeService.get().getServiceNames().get("en"), is(nameToUpdate));
        verify(serviceDao).merge(serviceEntity);
    }

    @Test
    public void shouldSuccess_updateMerchantDetails() throws ServiceNotFoundException {
        String name = "name";
        String telephoneNumber = "03069990000";
        String addressLine1 = "something";
        String addressLine2 = "something";
        String addressCity = "something";
        String addressPostcode = "something";
        String addressCountry = "something";
        String email = "dd-merchant@example.com";

        MerchantDetailsEntity toUpdate = new MerchantDetailsEntity(
                name, telephoneNumber, addressLine1, addressLine2, addressCity, addressPostcode, addressCountry, email
        );
        UpdateMerchantDetailsRequest request = new UpdateMerchantDetailsRequest(
                name, telephoneNumber, addressLine1, addressLine2, addressCity, addressPostcode, addressCountry, email
        );
        ServiceEntity serviceEntity = mock(ServiceEntity.class);
        when(serviceDao.findByExternalId(SERVICE_ID)).thenReturn(of(serviceEntity));
        when(serviceEntity.toService()).thenReturn(Service.from());

        Service service = updater.doUpdateMerchantDetails(SERVICE_ID, request);

        assertNotNull(service);
        verify(serviceEntity).setMerchantDetailsEntity(toUpdate);
        verify(serviceDao).merge(serviceEntity);
    }

    @Test(expected = ServiceNotFoundException.class)
    public void shouldError_updateMerchantDetails_whenServiceNotFound() throws ServiceNotFoundException {
        String name = "name";
        String telephoneNumber = "03069990000";
        String addressLine1 = "something";
        String addressLine2 = "something";
        String addressCity = "something";
        String addressPostcode = "something";
        String addressCountry = "something";
        String email = "merchant@example.com";

        UpdateMerchantDetailsRequest request = new UpdateMerchantDetailsRequest(
                name, telephoneNumber, addressLine1, addressLine2, addressCity, addressPostcode, addressCountry, email
        );
        ServiceEntity serviceEntity = mock(ServiceEntity.class);

        when(serviceDao.findByExternalId(NON_EXISTENT_SERVICE_EXTERNAL_ID)).thenReturn(Optional.empty());
        when(serviceEntity.toService()).thenReturn(Service.from());

        updater.doUpdateMerchantDetails(NON_EXISTENT_SERVICE_EXTERNAL_ID, request);
    }

    @Test
    public void shouldSuccess_updateCustomBranding_whenBrandingProvided() {
        ServiceUpdateRequest request = mock(ServiceUpdateRequest.class);
        ServiceEntity serviceEntity = mock(ServiceEntity.class);
        Map<String, Object> customBranding = ImmutableMap.of("image_url", "image url", "css_url", "css url");

        when(request.getPath()).thenReturn("custom_branding");
        when(request.valueAsObject()).thenReturn(customBranding);
        when(serviceDao.findByExternalId(SERVICE_ID)).thenReturn(of(serviceEntity));
        when(serviceEntity.toService()).thenReturn(Service.from());

        Optional<Service> maybeService = updater.doUpdate(SERVICE_ID, request);

        assertThat(maybeService.isPresent(), is(true));
        verify(serviceEntity).setCustomBranding(customBranding);
        verify(serviceDao).merge(serviceEntity);
    }

    @Test
    public void shouldSuccess_updateCustomBranding_whenBrandingNotProvided() {
        ServiceUpdateRequest request = mock(ServiceUpdateRequest.class);
        ServiceEntity serviceEntity = mock(ServiceEntity.class);

        when(request.getPath()).thenReturn("custom_branding");
        when(request.valueAsObject()).thenReturn(null);
        when(serviceDao.findByExternalId(SERVICE_ID)).thenReturn(of(serviceEntity));
        when(serviceEntity.toService()).thenReturn(Service.from());

        Optional<Service> maybeService = updater.doUpdate(SERVICE_ID, request);

        assertThat(maybeService.isPresent(), is(true));
        verify(serviceEntity).setCustomBranding(null);
        verify(serviceDao).merge(serviceEntity);
    }

    @Test
    public void shouldSuccess_whenAddGatewayAccountToService() {
        ServiceUpdateRequest request = mock(ServiceUpdateRequest.class);
        ServiceEntity serviceEntity = mock(ServiceEntity.class);
        List<String> gatewayAccountIdsToUpdate = asList("1", "2");

        when(request.getPath()).thenReturn("gateway_account_ids");
        when(request.valueAsList()).thenReturn(gatewayAccountIdsToUpdate);
        when(serviceDao.findByExternalId(SERVICE_ID)).thenReturn(of(serviceEntity));
        when(serviceDao.checkIfGatewayAccountsUsed(gatewayAccountIdsToUpdate)).thenReturn(false);
        when(serviceEntity.toService()).thenReturn(Service.from());

        Optional<Service> maybeService = updater.doUpdate(SERVICE_ID, request);

        assertThat(maybeService.isPresent(), is(true));
        verify(serviceEntity).addGatewayAccountIds(gatewayAccountIdsToUpdate.toArray(new String[0]));
        verify(serviceDao).merge(serviceEntity);
    }

    @Test(expected = WebApplicationException.class)
    public void shouldError_IfAGatewayAccountAlreadyAssignedToAService() {
        ServiceUpdateRequest request = mock(ServiceUpdateRequest.class);
        ServiceEntity serviceEntity = mock(ServiceEntity.class);
        List<String> gatewayAccountIdsToUpdate = asList("1", "2");

        when(request.getPath()).thenReturn("gateway_account_ids");
        when(request.valueAsList()).thenReturn(gatewayAccountIdsToUpdate);
        when(serviceDao.findByExternalId(SERVICE_ID)).thenReturn(of(serviceEntity));
        when(serviceDao.checkIfGatewayAccountsUsed(gatewayAccountIdsToUpdate)).thenReturn(true);

        Optional<Service> maybeService = updater.doUpdate(SERVICE_ID, request);

        assertThat(maybeService.isPresent(), is(false));
        verify(serviceEntity, times(0)).addGatewayAccountIds(gatewayAccountIdsToUpdate.toArray(new String[0]));
        verify(serviceDao, times(0)).merge(serviceEntity);
    }

    @Test
    public void shouldUpdateMultilingualServiceNameSuccessfully() {
        String nameToUpdate = "new-cy-name";
        ServiceUpdateRequest request = serviceUpdateRequest("replace", "service_name/cy", nameToUpdate);
        ServiceEntity serviceEntity = mock(ServiceEntity.class);

        when(serviceDao.findByExternalId(SERVICE_ID)).thenReturn(of(serviceEntity));
        when(serviceEntity.toService()).thenReturn(Service.from());

        Optional<Service> maybeService = updater.doUpdate(SERVICE_ID, request);

        assertThat(maybeService.isPresent(), is(true));
        ServiceNameEntity serviceNameEntity = ServiceNameEntity.from(SupportedLanguage.WELSH, nameToUpdate);

        InOrder inOrder = inOrder(ignoreStubs(serviceDao, serviceEntity));
        inOrder.verify(serviceEntity).addOrUpdateServiceName(serviceNameEntity);
        inOrder.verify(serviceDao).merge(serviceEntity);
    }

    @Test
    public void shouldUpdateRedirectImmediatelySuccessfully() {
        ServiceUpdateRequest request = ServiceUpdateRequest.from(new ObjectNode(JsonNodeFactory.instance, ImmutableMap.of(
                "path", new TextNode("redirect_to_service_immediately_on_terminal_state"),
                "value", BooleanNode.valueOf(true),
                "op", new TextNode("replace"))));
        ServiceEntity serviceEntity = mock(ServiceEntity.class);

        when(serviceDao.findByExternalId(SERVICE_ID)).thenReturn(of(serviceEntity));
        when(serviceEntity.toService()).thenReturn(Service.from());

        Optional<Service> maybeService = updater.doUpdate(SERVICE_ID, request);

        assertThat(maybeService.isPresent(), is(true));
        verify(serviceEntity).setRedirectToServiceImmediatelyOnTerminalState(true);
        verify(serviceDao).merge(serviceEntity);
    }

    @Test
    public void shouldUpdateCollectBillingAddressSuccessfully() {
        ServiceUpdateRequest request = ServiceUpdateRequest.from(new ObjectNode(JsonNodeFactory.instance, ImmutableMap.of(
                "path", new TextNode("collect_billing_address"),
                "value", BooleanNode.valueOf(false),
                "op", new TextNode("replace"))));
        ServiceEntity serviceEntity = mock(ServiceEntity.class);

        when(serviceDao.findByExternalId(SERVICE_ID)).thenReturn(of(serviceEntity));
        when(serviceEntity.toService()).thenReturn(Service.from());

        Optional<Service> maybeService = updater.doUpdate(SERVICE_ID, request);

        assertThat(maybeService.isPresent(), is(true));
        InOrder inOrder = inOrder(serviceEntity, serviceDao);
        inOrder.verify(serviceEntity).setCollectBillingAddress(false);
        inOrder.verify(serviceDao).merge(serviceEntity);
    }

    @Test
    public void shouldUpdateCurrentGoLIveStageSuccessfully() {
        ServiceUpdateRequest request = serviceUpdateRequest("replace", "current_go_live_stage", valueOf(GoLiveStage.CHOSEN_PSP_STRIPE));
        ServiceEntity serviceEntity = mock(ServiceEntity.class);

        when(serviceDao.findByExternalId(SERVICE_ID)).thenReturn(of(serviceEntity));
        when(serviceEntity.toService()).thenReturn(Service.from());

        Optional<Service> maybeService = updater.doUpdate(SERVICE_ID, request);

        assertThat(maybeService.isPresent(), is(true));
        verify(serviceEntity).setCurrentGoLiveStage(GoLiveStage.CHOSEN_PSP_STRIPE);
        verify(serviceDao).merge(serviceEntity);
    }

    @Test
    public void shouldUpdateMerchantDetailsNameSuccessfully_WhenNoExistingMerchantDetails() {
        String name = "Cake service";
        ServiceUpdateRequest serviceRequest =
                serviceUpdateRequest("replace", "merchant_details/name", name);
        ServiceEntity serviceEntity = aServiceEntity()
                .withMerchantDetailsEntity(null)
                .build();

        when(serviceDao.findByExternalId(SERVICE_ID)).thenReturn(of(serviceEntity));

        Optional<Service> maybeService = updater.doUpdate(SERVICE_ID, serviceRequest);

        assertThat(maybeService.isPresent(), is(true));
        verify(serviceDao).merge(serviceEntity);
        assertThat(maybeService.get().getMerchantDetails().getName(), is(name));
    }

    @Test
    public void shouldUpdateMultipleMerchantDetailsSuccessfully_WhenNoExistingMerchantDetails() {
        String name = "Cake service";
        String addressLine1 = "1 Spider Lane";
        String addressLine2 = "Some";
        String addressCity = "where";
        String addressCountry = "over";
        String addressPostcode = "W10 5LA";
        String email = "someone@example.com";
        String telephoneNumber = "000";

        ArrayList<ServiceUpdateRequest> serviceUpdateRequests = newArrayList(
                serviceUpdateRequest("replace", "merchant_details/name", name),
                serviceUpdateRequest("replace", "merchant_details/address_line1", addressLine1),
                serviceUpdateRequest("replace", "merchant_details/address_line2", addressLine2),
                serviceUpdateRequest("replace", "merchant_details/address_city", addressCity),
                serviceUpdateRequest("replace", "merchant_details/address_country", addressCountry),
                serviceUpdateRequest("replace", "merchant_details/address_postcode", addressPostcode),
                serviceUpdateRequest("replace", "merchant_details/email", email),
                serviceUpdateRequest("replace", "merchant_details/telephone_number", telephoneNumber)
        );
        ServiceEntity serviceEntity = aServiceEntity()
                .withMerchantDetailsEntity(null)
                .build();

        when(serviceDao.findByExternalId(SERVICE_ID)).thenReturn(of(serviceEntity));

        Optional<Service> maybeService = updater.doUpdate(SERVICE_ID, serviceUpdateRequests);

        assertThat(maybeService.isPresent(), is(true));
        verify(serviceDao, times(8)).merge(serviceEntity);
        assertThat(maybeService.get().getMerchantDetails().getName(), is(name));
        assertThat(maybeService.get().getMerchantDetails().getAddressLine1(), is(addressLine1));
        assertThat(maybeService.get().getMerchantDetails().getAddressLine2(), is(addressLine2));
        assertThat(maybeService.get().getMerchantDetails().getAddressCity(), is(addressCity));
        assertThat(maybeService.get().getMerchantDetails().getAddressCountry(), is(addressCountry));
        assertThat(maybeService.get().getMerchantDetails().getAddressPostcode(), is(addressPostcode));
        assertThat(maybeService.get().getMerchantDetails().getEmail(), is(email));
        assertThat(maybeService.get().getMerchantDetails().getTelephoneNumber(), is(telephoneNumber));
    }


    @Test
    public void shouldUpdateMerchantDetailsAddressLine1Successfully_WhenExistingMerchantDetails() {
        String updatedAddressLine1 = "1 Spider Lane";
        ServiceUpdateRequest request = serviceUpdateRequest("replace", "merchant_details/address_line1", updatedAddressLine1);
        MerchantDetailsEntity merchantDetails = aMerchantDetailsEntity().build();
        ServiceEntity serviceEntity = aServiceEntity()
                .withMerchantDetailsEntity(merchantDetails)
                .build();

        when(serviceDao.findByExternalId(SERVICE_ID)).thenReturn(of(serviceEntity));

        Optional<Service> maybeService = updater.doUpdate(SERVICE_ID, request);

        assertThat(maybeService.isPresent(), is(true));
        verify(serviceDao).merge(serviceEntity);
        assertThat(maybeService.get().getMerchantDetails().getName(), is("test-name"));
        assertThat(maybeService.get().getMerchantDetails().getAddressLine1(), is(updatedAddressLine1));
    }

    private static ServiceUpdateRequest serviceUpdateRequest(String op, String path, String value) {
        return ServiceUpdateRequest.from(
                new ObjectNode(JsonNodeFactory.instance, ImmutableMap.of(
                        "op", new TextNode(op),
                        "path", new TextNode(path),
                        "value", new TextNode(value))));
    }
}
