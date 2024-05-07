package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import uk.gov.pay.adminusers.infra.AppWithPostgresAndSqsRule;
import uk.gov.pay.adminusers.model.GoLiveStage;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.service.PasswordHasher;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.ForgottenPasswordDbFixture.aForgottenPasswordDbFixture;
import static uk.gov.pay.adminusers.fixtures.InviteDbFixture.inviteDbFixture;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public abstract class ContractTest {

    @ClassRule
    public static AppWithPostgresAndSqsRule app = new AppWithPostgresAndSqsRule();

    @TestTarget
    public static Target target;

    private static final PasswordHasher PASSWORD_HASHER = new PasswordHasher();
    private static DatabaseTestHelper dbHelper;

    @BeforeClass
    public static void setUpService() {
        target = new HttpTarget(app.getLocalPort());
        dbHelper = app.getDatabaseTestHelper();
        // make sure we create services(including gateway account ids) before users
        serviceDbFixture(dbHelper).withGatewayAccountIds("268").insertService();
    }

    @Before
    public void resetDatabase() {
        dbHelper.truncateAllData();
    }

}
