package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import uk.gov.pay.adminusers.app.util.RandomIdGenerator;
import uk.gov.pay.adminusers.fixtures.RoleDbFixture;
import uk.gov.pay.adminusers.fixtures.ServiceDbFixture;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;
import uk.gov.pay.adminusers.infra.DropwizardAppWithPostgresRule;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.GoLiveStage;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.service.PasswordHasher;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

@RunWith(PactRunner.class)
@Provider("adminusers")
@PactBroker(scheme = "https", host = "pact-broker-test.cloudapps.digital", tags = {"${PACT_CONSUMER_TAG}", "test", "staging", "production"},
        authentication = @PactBrokerAuth(username = "${PACT_BROKER_USERNAME}", password = "${PACT_BROKER_PASSWORD}"))
public class ProviderContractTest {

    @ClassRule
    public static DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @TestTarget
    public static Target target;

    private static final PasswordHasher passwordHasher = new PasswordHasher();
    private static DatabaseTestHelper dbHelper;

    @BeforeClass
    public static void setUpService() throws Exception {
        target = new HttpTarget(app.getLocalPort());
        dbHelper = app.getDatabaseTestHelper();
        // make sure we create services(including gateway account ids) before users
        serviceDbFixture(dbHelper).withGatewayAccountIds("268").insertService();
    }

    @Before
    public void resetDatabase() {
        dbHelper.truncateAllData();
    }

    @State("a valid forgotten password entry and a related user exists")
    public void aUserExistsWithAForgottenPasswordRequest() throws Exception {
        String code = "avalidforgottenpasswordtoken";
        String userExternalId = RandomIdGenerator.randomUuid();
        createUserWithinAService(userExternalId, RandomIdGenerator.randomUuid(), "password");
        List<Map<String, Object>> userByExternalId = dbHelper.findUserByExternalId(userExternalId);
        dbHelper.add(ForgottenPassword.forgottenPassword(code, userExternalId), (Integer) userByExternalId.get(0).get("id"));
    }

    @State("a user exists with max login attempts")
    public void aUserExistsWithMaxLoginAttempts() throws Exception {
        String username = "user-login-attempts-max";
        createUserWithinAService(RandomIdGenerator.randomUuid(), username, "password");
        dbHelper.updateLoginCount(username, 10);
    }

    @State("a forgotten password entry exist")
    public void aForgottenPasswordEntryExist() throws Exception {
        String code = "existing-code";
        String existingUserExternalId = "7d19aff33f8948deb97ed16b2912dcd3";
        List<Map<String, Object>> userByName = dbHelper.findUserByExternalId(existingUserExternalId);
        dbHelper.add(ForgottenPassword.forgottenPassword(code, existingUserExternalId), (Integer) userByName.get(0).get("id"));
    }

    @State("a user and user admin exists in service with the given ids before a delete operation")
    public void aUserAndUserAdminExistBeforeADelete() throws Exception {

        String existingUserExternalId = "pact-delete-user-id";
        String existingUserRemoverExternalId = "pact-delete-remover-id";
        String existingServiceExternalId = "pact-delete-service-id";

        Service service = ServiceDbFixture.serviceDbFixture(dbHelper).withExternalId(existingServiceExternalId).insertService();
        Role role = RoleDbFixture.roleDbFixture(dbHelper).insertAdmin();

        String username1 = randomUuid();
        String email1 = username1 + "@example.com";
        UserDbFixture.userDbFixture(dbHelper).withExternalId(existingUserExternalId).withServiceRole(service, role.getId()).withUsername(username1).withEmail(email1).insertUser();
        String username2 = randomUuid();
        String email2 = username2 + "@example.com";
        UserDbFixture.userDbFixture(dbHelper).withExternalId(existingUserRemoverExternalId).withServiceRole(service, role.getId()).withUsername(username2).withEmail(email2).insertUser();
    }

    @State("a user exists but not the remover before a delete operation")
    public void aUserExistButRemoverBeforeADelete() throws Exception {

        String existingUserExternalId = "pact-user-no-remover-test";
        String existingServiceExternalId = "pact-service-no-remover-test";

        Service service = ServiceDbFixture.serviceDbFixture(dbHelper).withExternalId(existingServiceExternalId).insertService();
        Role role = RoleDbFixture.roleDbFixture(dbHelper).insertAdmin();

        String username = randomUuid();
        String email = username + "@example.com";
        UserDbFixture.userDbFixture(dbHelper).withExternalId(existingUserExternalId).withServiceRole(service, role.getId()).withUsername(username).withEmail(email).insertUser();
    }

    @State({"a forgotten password does not exists",
            "a user does not exist",
            "a user exists",
            "no user exits with the given name",
            "a user exits with the given name",
            "a valid (non-expired) forgotten password entry does not exist",
            "default",
            "a user exist",
            "a user exists with a given username password",
            "a user not exists with a given username password",
            "a user not exists with a given username password",
            "no user exists with the given external id"
    })
    public void noSetUp() {
    }

    @State("a user exists with the given external id 7d19aff33f8948deb97ed16b2912dcd3")
    public void aUserExistsWithGivenExternalId() {
        createUserWithinAService("7d19aff33f8948deb97ed16b2912dcd3", "existing-user", "password");
    }

    @State("a service exists with external id cp5wa and billing address collection enabled")
    public void aServiceExistsWithBillingAddressCollectionEnabled() {
        serviceDbFixture(dbHelper)
                .withExternalId("cp5wa")
                .withCollectBillingAddress(true)
                .insertService();
    }

    @State("a service exists with external id rtglNotStarted and go live stage equals to NOT_STARTED")
    public void aServiceExistsWithNotStartedGoLiveStage() {
        serviceDbFixture(dbHelper)
                .withExternalId("rtglNotStarted")
                .withGoLiveStage(GoLiveStage.NOT_STARTED)
                .insertService();
    }

    @State("a service exists with external id rtglEnteredOrgName and go live stage equals to ENTERED_ORGANISATION_NAME")
    public void aServiceExistsWithEnteredOrganisationNameGoLiveStage() {
        serviceDbFixture(dbHelper)
                .withExternalId("rtglEnteredOrgName")
                .withGoLiveStage(GoLiveStage.ENTERED_ORGANISATION_NAME)
                .insertService();
    }

    @State("a service exists with external id rtglChosenPspStripe and go live stage equals to CHOSEN_PSP_STRIPE")
    public void aServiceExistsWithChosenPspStripeGoLiveStage() {
        serviceDbFixture(dbHelper)
                .withExternalId("rtglChosenPspStripe")
                .withGoLiveStage(GoLiveStage.CHOSEN_PSP_STRIPE)
                .insertService();
    }

    @State("a service exists with external id rtglChosenPspWorldPay and go live stage equals to CHOSEN_PSP_WORLDPAY")
    public void aServiceExistsWithChosenPspWorldPayGoLiveStage() {
        serviceDbFixture(dbHelper)
                .withExternalId("rtglChosenPspWorldPay")
                .withGoLiveStage(GoLiveStage.CHOSEN_PSP_WORLDPAY)
                .insertService();
    }

    @State("a service exists with external id rtglChosenPspSmartPay and go live stage equals to CHOSEN_PSP_SMARTPAY")
    public void aServiceExistsWithChosenPspSmartPayGoLiveStage() {
        serviceDbFixture(dbHelper)
                .withExternalId("rtglChosenPspSmartPay")
                .withGoLiveStage(GoLiveStage.CHOSEN_PSP_SMARTPAY)
                .insertService();
    }

    @State("a service exists with external id rtglChosenPspEpdq and go live stage equals to CHOSEN_PSP_EPDQ")
    public void aServiceExistsWithChosenPspEpdqGoLiveStage() {
        serviceDbFixture(dbHelper)
                .withExternalId("rtglChosenPspEpdq")
                .withGoLiveStage(GoLiveStage.CHOSEN_PSP_EPDQ)
                .insertService();
    }

    @State("a service exists with external id rtglTermsOkStripe and go live stage equals to TERMS_AGREED_STRIPE")
    public void aServiceExistsWithTermsAgreedStripeGoLiveStage() {
        serviceDbFixture(dbHelper)
                .withExternalId("rtglTermsOkStripe")
                .withGoLiveStage(GoLiveStage.TERMS_AGREED_STRIPE)
                .insertService();
    }

    @State("a service exists with external id rtglTermsOkWorldPay and go live stage equals to TERMS_AGREED_WORLDPAY")
    public void aServiceExistsWithTermsAgreedWorldPayGoLiveStage() {
        serviceDbFixture(dbHelper)
                .withExternalId("rtglTermsOkWorldPay")
                .withGoLiveStage(GoLiveStage.TERMS_AGREED_WORLDPAY)
                .insertService();
    }

    @State("a service exists with external id rtglTermsOkSmartPay and go live stage equals to TERMS_AGREED_SMARTPAY")
    public void aServiceExistsWithTermsAgreedSmartPayGoLiveStage() {
        serviceDbFixture(dbHelper)
                .withExternalId("rtglTermsOkSmartPay")
                .withGoLiveStage(GoLiveStage.TERMS_AGREED_SMARTPAY)
                .insertService();
    }

    @State("a service exists with external id rtglTermsOkEpdq and go live stage equals to TERMS_AGREED_EPDQ")
    public void aServiceExistsWithTermsAgreedEpdqGoLiveStage() {
        serviceDbFixture(dbHelper)
                .withExternalId("rtglTermsOkEpdq")
                .withGoLiveStage(GoLiveStage.TERMS_AGREED_EPDQ)
                .insertService();
    }

    @State("a service exists with external id rtglDenied and go live stage equals to DENIED")
    public void aServiceExistsWithDeniedGoLiveStage() {
        serviceDbFixture(dbHelper)
                .withExternalId("rtglDenied")
                .withGoLiveStage(GoLiveStage.DENIED)
                .insertService();
    }

    @State("a service exists with external id rtglLive and go live stage equals to LIVE")
    public void aServiceExistsWithLiveGoLiveStage() {
        serviceDbFixture(dbHelper)
                .withExternalId("rtglLive")
                .withGoLiveStage(GoLiveStage.LIVE)
                .insertService();
    }

    private static void createUserWithinAService(String externalId, String username, String password) {
        String gatewayAccount1 = randomNumeric(5);
        String gatewayAccount2 = randomNumeric(5);
        Role role = Role.role(randomInt(), "admin", "Administrator");
        roleDbFixture(dbHelper).insert(role,
                Permission.permission(randomInt(), "perm-1", "permission-1-description"),
                Permission.permission(randomInt(), "perm-2", "permission-2-description"),
                Permission.permission(randomInt(), "perm-3", "permission-3-description"));
        Service service = serviceDbFixture(dbHelper).withGatewayAccountIds(gatewayAccount1, gatewayAccount2).insertService();

        UserDbFixture.userDbFixture(dbHelper)
                .withExternalId(externalId)
                .withUsername(username)
                .withPassword(passwordHasher.hash(password))
                .withEmail("user-" + username + "@example.com")
                .withGatewayAccountIds(Arrays.asList(gatewayAccount1, gatewayAccount2))
                .withTelephoneNumber("45334534634")
                .withOtpKey("34f34")
                .withProvisionalOtpKey("94423")
                .withServiceRole(service.getId(), role.getId())
                .insertUser();
    }
}
