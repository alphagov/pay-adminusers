package uk.gov.pay.adminusers.model;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.commons.model.SupportedLanguage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ServiceTest {

    @Test
    public void fromWithNoEnglishServiceNameSetsNameAsEnglishServiceName() {
        ServiceNameEntity welshServiceName = ServiceNameEntity.from(SupportedLanguage.WELSH, "Welsh Service Name");
        
        Service service = Service.from("Service Name", ImmutableMap.of(SupportedLanguage.WELSH, welshServiceName));
        
        assertThat(service.getServiceNames().get(SupportedLanguage.ENGLISH.toString()), is("Service Name"));
        assertThat(service.getServiceNames().get(SupportedLanguage.WELSH.toString()), is("Welsh Service Name"));
    }

}
