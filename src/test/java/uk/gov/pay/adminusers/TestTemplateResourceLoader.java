package uk.gov.pay.adminusers;

import static io.dropwizard.testing.FixtureHelpers.fixture;

public class TestTemplateResourceLoader {
    private static final String TEMPLATE_BASE_NAME = "templates";
    
    public static final String DISPUTE_CREATED_EVENT = TEMPLATE_BASE_NAME + "/events/dispute_created_event.json";

    public static final String DISPUTE_LOST_EVENT = TEMPLATE_BASE_NAME + "/events/dispute_lost_event.json";

    public static final String DISPUTE_CREATED_SNS_MESSAGE = TEMPLATE_BASE_NAME + "/sns/dispute_created_sns_message.json";


    public static String load(String location) {
        return fixture(location);
    }

}
