package uk.gov.pay.adminusers.unit.service;

import io.restassured.path.json.JsonPath;
import uk.gov.pay.adminusers.persistence.dao.ServiceDao;
import uk.gov.pay.adminusers.service.LinksBuilder;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

abstract class ServiceResourceBaseTest {
    /* default */ static final String GATEWAY_ACCOUNT_ID = "some-gateway-account-id";
    /* default */ static final String CY_SERVICE_NAME = "some-welsh-service-name";
    /* default */ static final String EN_SERVICE_NAME = "some-test-service-name";

    /* default */ static ServiceDao mockedServiceDao = mock(ServiceDao.class);
    private static final String HTTPS_BASE_URL = "https://base-url";
    /* default */ static final LinksBuilder LINKS_BUILDER = new LinksBuilder(HTTPS_BASE_URL);

    /* default */ static void assertLinks(String serviceExternalId, JsonPath json) {
        assertThat(json.getList("_links"), hasSize(1));
        assertThat(json.get("_links[0].href"), is(HTTPS_BASE_URL + "/v1/api/services/" + serviceExternalId));
        assertThat(json.get("_links[0].method"), is("GET"));
        assertThat(json.get("_links[0].rel"), is("self"));
    }

    /* default */ static void assertEnServiceNameJson(String name, JsonPath json) {
        assertThat(json.getMap("service_name"), hasKey(SupportedLanguage.ENGLISH.toString()));
        assertThat(json.get("service_name.en"), is(name));
    }

    /* default */ static void assertCyServiceNameJson(String cyName, JsonPath json) {
        assertThat(json.getMap("service_name"), hasKey(SupportedLanguage.WELSH.toString()));
        assertThat(json.get("service_name.cy"), is(cyName));
    }
}
