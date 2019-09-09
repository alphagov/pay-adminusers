package uk.gov.pay.adminusers.persistence.entity;

import org.junit.Test;
import uk.gov.pay.adminusers.model.GoLiveStage;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.commons.model.SupportedLanguage;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ServiceEntityTest {

    private static final String ENGLISH_SERVICE_NAME = "Apply for your licence";
    private static final String WELSH_SERVICE_NAME = "Gwneud cais am eich trwydded";

    @Test
    public void shouldUpdateExistingServiceName() {
        ServiceEntity serviceEntity = ServiceEntityBuilder.aServiceEntity().build();

        assertThat(serviceEntity.getServiceNames().size(), is(1));
        assertThat(serviceEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName(), is(Service.DEFAULT_NAME_VALUE));

        serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(SupportedLanguage.ENGLISH, ENGLISH_SERVICE_NAME));

        assertThat(serviceEntity.getServiceNames().size(), is(1));
        assertThat(serviceEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName(), is(ENGLISH_SERVICE_NAME));
    }

    @Test
    public void shouldAddNewServiceName() {
        ServiceEntity serviceEntity = ServiceEntityBuilder.aServiceEntity().build();

        assertThat(serviceEntity.getServiceNames().size(), is(1));
        assertThat(serviceEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName(), is(Service.DEFAULT_NAME_VALUE));

        serviceEntity.addOrUpdateServiceName(ServiceNameEntity.from(SupportedLanguage.WELSH, WELSH_SERVICE_NAME));

        assertThat(serviceEntity.getServiceNames().size(), is(2));
        assertThat(serviceEntity.getServiceNames().get(SupportedLanguage.ENGLISH).getName(), is(Service.DEFAULT_NAME_VALUE));
        assertThat(serviceEntity.getServiceNames().get(SupportedLanguage.WELSH).getName(), is(WELSH_SERVICE_NAME));
    }

    @Test
    public void shouldCreateEntity_withNotStartedAsDefault() {
        Service service = ServiceEntityBuilder.aServiceEntity().build().toService();
        assertThat(service.getGoLiveStage(), is(GoLiveStage.NOT_STARTED));
    }

}
