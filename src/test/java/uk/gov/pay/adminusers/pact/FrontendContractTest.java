package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.provider.junit.Consumer;
import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import uk.gov.pay.adminusers.infra.DropwizardAppWithPostgresRule;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

@RunWith(PactRunner.class)
@Provider("adminusers")
@Consumer("frontend")
//@PactBroker(scheme = "https", host = "pact-broker-test.cloudapps.digital", tags = {"${PACT_CONSUMER_TAG}", "test", "staging", "production"},
//        authentication = @PactBrokerAuth(username = "${PACT_BROKER_USERNAME}", password = "${PACT_BROKER_PASSWORD}"))
@PactFolder("pacts")
public class FrontendContractTest {

    @ClassRule
    public static DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @TestTarget
    public static Target target;
    private static DatabaseTestHelper dbHelper;
    
    @BeforeClass
    public static void setUpService() {
        target = new HttpTarget(app.getLocalPort());
        dbHelper = app.getDatabaseTestHelper();
    }

    @Before
    public void resetDatabase() {
        dbHelper.truncateAllData();
    }

    @State({"a service exists with gateway account id 101"})
    public void aServiceExists() {
        serviceDbFixture(dbHelper)
                .withExternalId("a-service-id")
                .withGatewayAccountIds("101")
                .insertService();
    }
}
