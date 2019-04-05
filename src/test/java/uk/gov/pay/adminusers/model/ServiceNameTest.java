package uk.gov.pay.adminusers.model;

import org.junit.Test;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.pay.commons.model.SupportedLanguage;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class ServiceNameTest {

    private static final String ENGLISH_SERVICE_NAME = "Apply for your licence";
    private static final String WELSH_SERVICE_NAME = "Gwneud cais am eich trwydded";
    
    @Test
    public void shouldCreateWithJustEnglishServiceName() {
        ServiceName serviceName = new ServiceName(ENGLISH_SERVICE_NAME);
        
        assertThat(serviceName.getEnglish(), is(ENGLISH_SERVICE_NAME));
        assertThat(serviceName.getEnglishAndTranslations().size(), is(1));
        assertThat(serviceName.getEnglishAndTranslations().get(SupportedLanguage.ENGLISH), is(ENGLISH_SERVICE_NAME));
    }

    @Test
    public void shouldCreateWithEnglishAndNonEnglishServiceName() {
        ServiceName serviceName = new ServiceName(ENGLISH_SERVICE_NAME, Map.of(SupportedLanguage.WELSH, WELSH_SERVICE_NAME));

        assertThat(serviceName.getEnglish(), is(ENGLISH_SERVICE_NAME));
        assertThat(serviceName.getEnglishAndTranslations().size(), is(2));
        assertThat(serviceName.getEnglishAndTranslations().get(SupportedLanguage.ENGLISH), is(ENGLISH_SERVICE_NAME));
        assertThat(serviceName.getEnglishAndTranslations().get(SupportedLanguage.WELSH), is(WELSH_SERVICE_NAME));
    }

    @Test
    public void shouldIgnoreNonEnglishServiceNameIfEmpty() {
        ServiceName serviceName = new ServiceName(ENGLISH_SERVICE_NAME, Map.of(SupportedLanguage.WELSH, ""));

        assertThat(serviceName.getEnglish(), is(ENGLISH_SERVICE_NAME));
        assertThat(serviceName.getEnglishAndTranslations().size(), is(1));
        assertThat(serviceName.getEnglishAndTranslations().get(SupportedLanguage.ENGLISH), is(ENGLISH_SERVICE_NAME));
        assertThat(serviceName.getEnglishAndTranslations().get(SupportedLanguage.WELSH), is(nullValue()));
    }

    @Test
    public void shouldIgnoreNonEnglishServiceNameIfBlank() {
        ServiceName serviceName = new ServiceName(ENGLISH_SERVICE_NAME, Map.of(SupportedLanguage.WELSH, "  "));

        assertThat(serviceName.getEnglish(), is(ENGLISH_SERVICE_NAME));
        assertThat(serviceName.getEnglishAndTranslations().size(), is(1));
        assertThat(serviceName.getEnglishAndTranslations().get(SupportedLanguage.ENGLISH), is(ENGLISH_SERVICE_NAME));
        assertThat(serviceName.getEnglishAndTranslations().get(SupportedLanguage.WELSH), is(nullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowIfNullEnglishServiceName() {
        new ServiceName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfEnglishServiceNameIncludedInMap() {
        new ServiceName(ENGLISH_SERVICE_NAME, Map.of(SupportedLanguage.WELSH, WELSH_SERVICE_NAME, SupportedLanguage.ENGLISH, ENGLISH_SERVICE_NAME));
    }

    @Test
    public void shouldConvertFromEnglishServiceNameEntity() {
        ServiceName serviceName = ServiceName.from(List.of(ServiceNameEntity.from(SupportedLanguage.ENGLISH, ENGLISH_SERVICE_NAME)));

        assertThat(serviceName.getEnglish(), is(ENGLISH_SERVICE_NAME));
        assertThat(serviceName.getEnglishAndTranslations().size(), is(1));
        assertThat(serviceName.getEnglishAndTranslations().get(SupportedLanguage.ENGLISH), is(ENGLISH_SERVICE_NAME));
    }

    @Test
    public void shouldConvertFromEnglishServiceNameEntityAndNonEnglishServiceNameEntity() {
        ServiceName serviceName = ServiceName.from(List.of(ServiceNameEntity.from(SupportedLanguage.ENGLISH, ENGLISH_SERVICE_NAME),
                ServiceNameEntity.from(SupportedLanguage.WELSH, WELSH_SERVICE_NAME)));

        assertThat(serviceName.getEnglish(), is(ENGLISH_SERVICE_NAME));
        assertThat(serviceName.getEnglishAndTranslations().size(), is(2));
        assertThat(serviceName.getEnglishAndTranslations().get(SupportedLanguage.ENGLISH), is(ENGLISH_SERVICE_NAME));
        assertThat(serviceName.getEnglishAndTranslations().get(SupportedLanguage.WELSH), is(WELSH_SERVICE_NAME));
    }

    @Test
    public void shouldConvertFromServiceNameEntitiesIgnoringNonEnglishServiceNameIfEmpty() {
        ServiceName serviceName = ServiceName.from(List.of(ServiceNameEntity.from(SupportedLanguage.ENGLISH, ENGLISH_SERVICE_NAME),
                ServiceNameEntity.from(SupportedLanguage.WELSH, "")));

        assertThat(serviceName.getEnglish(), is(ENGLISH_SERVICE_NAME));
        assertThat(serviceName.getEnglishAndTranslations().size(), is(1));
        assertThat(serviceName.getEnglishAndTranslations().get(SupportedLanguage.ENGLISH), is(ENGLISH_SERVICE_NAME));
        assertThat(serviceName.getEnglishAndTranslations().get(SupportedLanguage.WELSH), is(nullValue()));
    }

    @Test
    public void shouldConvertFromServiceNameEntitiesIgnoringNonEnglishServiceNameIfBlank() {
        ServiceName serviceName = ServiceName.from(List.of(ServiceNameEntity.from(SupportedLanguage.ENGLISH, ENGLISH_SERVICE_NAME),
                ServiceNameEntity.from(SupportedLanguage.WELSH, "  ")));

        assertThat(serviceName.getEnglish(), is(ENGLISH_SERVICE_NAME));
        assertThat(serviceName.getEnglishAndTranslations().size(), is(1));
        assertThat(serviceName.getEnglishAndTranslations().get(SupportedLanguage.ENGLISH), is(ENGLISH_SERVICE_NAME));
        assertThat(serviceName.getEnglishAndTranslations().get(SupportedLanguage.WELSH), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfWhenConvertingFromServiceNameEntitiesIfNoEnglishServiceName() {
        ServiceName.from(List.of(ServiceNameEntity.from(SupportedLanguage.WELSH, "  ")));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldReturnUnmodifiableMap() {
        ServiceName serviceName = new ServiceName(ENGLISH_SERVICE_NAME, Map.of(SupportedLanguage.WELSH, WELSH_SERVICE_NAME));

        serviceName.getEnglishAndTranslations().put(SupportedLanguage.WELSH, "Sneakily try to add something");
    }
    
}
