package uk.gov.pay.adminusers.persistence.dao;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import uk.gov.pay.adminusers.infra.DropwizardAppWithPostgresRule;
import uk.gov.pay.adminusers.infra.GuicedTestEnvironment;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;
public class DaoTestBase {

    @ClassRule
    public static final DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    protected static DatabaseTestHelper databaseHelper;
    protected static GuicedTestEnvironment env;

    @BeforeClass
    public static void setup()  {
        databaseHelper = app.getDatabaseTestHelper();

        env = GuicedTestEnvironment.from(app.getJpaModule()).start();
    }

    @AfterClass
    public static void cleanUp() {
        env.stop();
    }
}
