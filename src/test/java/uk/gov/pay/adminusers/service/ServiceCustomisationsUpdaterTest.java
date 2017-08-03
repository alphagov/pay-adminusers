package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceCustomisations;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceCustomisationsUpdaterTest {

    @Mock
    ServiceDao serviceDao;

    ServiceCustomisationsUpdater serviceCustomisationsUpdater;

    @Before
    public void before() throws Exception {
        serviceCustomisationsUpdater = new ServiceCustomisationsUpdater(serviceDao, new LinksBuilder("http://localhost"));
    }

    @Test
    public void shouldReturnEmpty_ifServiceNotFound() throws Exception {
        String serviceExternalId = "non-existent-id";
        when(serviceDao.findByExternalId(serviceExternalId)).thenReturn(Optional.empty());
        Optional<Service> serviceOptional = serviceCustomisationsUpdater.doUpdate(serviceExternalId, new ServiceCustomisations("", ""));

        assertThat(serviceOptional.isPresent(), is(false));
    }

    @Test
    public void shouldReturnServiceWithCustomisations_ifFound() throws Exception {
        String serviceExternalId = "existing-id";
        ServiceEntity serviceEntity = new ServiceEntity();
        serviceEntity.setExternalId(serviceExternalId);
        serviceEntity.setName("service name");

        assertThat(serviceEntity.getServiceCustomisationEntity(),is(nullValue()));

        when(serviceDao.findByExternalId(serviceExternalId)).thenReturn(Optional.of(serviceEntity));

        Optional<Service> serviceOptional = serviceCustomisationsUpdater.doUpdate(serviceExternalId, new ServiceCustomisations("red", "http://some.url"));

        assertThat(serviceOptional.isPresent(), is(true));
        assertThat(serviceOptional.get().getServiceCustomisations().getBannerColour(), is("red"));
        assertThat(serviceOptional.get().getServiceCustomisations().getLogoUrl(), is("http://some.url"));
    }
}
