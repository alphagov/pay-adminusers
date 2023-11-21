package uk.gov.pay.adminusers.expungeandarchive.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.adminusers.client.ledger.model.LedgerSearchTransactionsResponse;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;
import uk.gov.pay.adminusers.fixtures.ForgottenPasswordDbFixture;
import uk.gov.pay.adminusers.fixtures.RoleDbFixture;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;
import uk.gov.pay.adminusers.infra.LedgerStub;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.resources.IntegrationTest;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.valueOf;
import static java.time.ZoneOffset.UTC;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.pay.adminusers.fixtures.ForgottenPasswordDbFixture.aForgottenPasswordDbFixture;
import static uk.gov.pay.adminusers.fixtures.InviteDbFixture.inviteDbFixture;
import static uk.gov.pay.adminusers.fixtures.LedgerSearchTransactionsResponseFixture.aLedgerSearchTransactionsResponseFixture;
import static uk.gov.pay.adminusers.fixtures.LedgerTransactionFixture.aLedgerTransactionFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

class ExpungeAndArchiveHistoricalDataResourceIT extends IntegrationTest {

    ZonedDateTime now = ZonedDateTime.now(UTC);

    @RegisterExtension
    static WireMockExtension wireMockExtension = WireMockExtension.newInstance()
            .options(wireMockConfig().port(APP.getWireMockPort()))
            .build();

    LedgerStub ledgerStub;

    @BeforeEach
    void setUp() {
        databaseHelper.truncateAllData();
        ledgerStub = new LedgerStub(wireMockExtension);
    }

    @Test
    void shouldDeleteHistoricalUsers() {
        User userWithCreatedDate = insertUser(now.minusYears(5), null);
        User userWithLastLoggedInDate = insertUser(now.minusYears(10), now.minusYears(4));
        User userWithRecentLoggedInDateAndShouldNotBeDeleted = insertUser(now.minusYears(2), now.minusYears(1));
        User userWithRecentCreatedDateAndShouldNotBeDeleted = insertUser(now.minusYears(2), null);

        assertUserExists(userWithCreatedDate.getId(), userWithLastLoggedInDate.getId(),
                userWithRecentLoggedInDateAndShouldNotBeDeleted.getId(), userWithRecentCreatedDateAndShouldNotBeDeleted.getId());

        givenSetup().when()
                .contentType(JSON)
                .accept(JSON)
                .post("v1/tasks/expunge-and-archive-historical-data")
                .then()
                .statusCode(200);

        assertUserNotExists(userWithCreatedDate.getId(), userWithLastLoggedInDate.getId());
        assertUserExists(userWithRecentLoggedInDateAndShouldNotBeDeleted.getId(), userWithRecentCreatedDateAndShouldNotBeDeleted.getId());
    }

    @Test
    void shouldDeleteHistoricalInvitesAndForgottenPasswords() {
        User userToBeDeleted = userDbFixture(databaseHelper)
                .withCreatedAt(now.minusYears(5))
                .insertUser();
        User userThatWontBeDeleted = userDbFixture(databaseHelper)
                .withCreatedAt(now)
                .insertUser();

        ForgottenPasswordDbFixture forgottenPasswordDbFixture = insertForgottenPassword(userToBeDeleted.getId(), now.minusYears(5));
        ForgottenPasswordDbFixture forgottenPasswordThatShouldNotBeDeleted = insertForgottenPassword(userThatWontBeDeleted.getId(), now);
        String code = insertInvite(userToBeDeleted.getId(), now.minusYears(5));
        String codeThatShouldNotBeDeleted = insertInvite(userThatWontBeDeleted.getId(), now);

        assertForgottenPasswordsExist(forgottenPasswordDbFixture.getId(), forgottenPasswordThatShouldNotBeDeleted.getId());
        assertInvitesExist(code, codeThatShouldNotBeDeleted);

        givenSetup().when()
                .contentType(JSON)
                .accept(JSON)
                .post("v1/tasks/expunge-and-archive-historical-data")
                .then()
                .statusCode(200);

        assertForgottenPasswordsExist(forgottenPasswordThatShouldNotBeDeleted.getId());
        assertInvitesExist(codeThatShouldNotBeDeleted);

        List<Map<String, Object>> forgottenPasswordById = databaseHelper.findForgottenPasswordById((Integer) forgottenPasswordDbFixture.getId());
        assertTrue(forgottenPasswordById.isEmpty());

        Optional<Map<String, Object>> inviteByCode = databaseHelper.findInviteByCode(code);
        assertTrue(inviteByCode.isEmpty());

        assertUserNotExists(userToBeDeleted.getId());
        assertUserExists(userThatWontBeDeleted.getId());
    }

    @Test
    void shouldArchiveHistoricalServicesAndDetachUsers() throws JsonProcessingException {
        Service service = serviceDbFixture(databaseHelper)
                .withCreatedDate(now.minusYears(8))
                .withGatewayAccountIds(valueOf(nextInt()))
                .insertService();
        Role role = RoleDbFixture.roleDbFixture(databaseHelper).insertRole();
        User user = UserDbFixture.userDbFixture(databaseHelper).withServiceRole(service, role.getId()).insertUser();

        LedgerTransaction ledgerTransaction = aLedgerTransactionFixture()
                .withCreatedDate(now.minusYears(8))
                .build();
        LedgerSearchTransactionsResponse searchTransactionsResponse = aLedgerSearchTransactionsResponseFixture()
                .withTransactionList(List.of(ledgerTransaction))
                .build();
        ledgerStub.returnLedgerTransactionsForSearch(service.getGatewayAccountIds().get(0), searchTransactionsResponse);

        Map<String, Object> serviceAttributes = databaseHelper.findServiceByExternalId(service.getExternalId()).get(0);
        assertThat(serviceAttributes.get("external_id"), is(service.getExternalId()));

        List<Map<String, Object>> serviceRoles = databaseHelper.findServiceRoleForUser(user.getId());
        assertThat(serviceRoles.size(), is(1));

        givenSetup().when()
                .contentType(JSON)
                .accept(JSON)
                .post("v1/tasks/expunge-and-archive-historical-data")
                .then()
                .statusCode(200);

        List<Map<String, Object>> services = databaseHelper.findServiceByExternalId(service.getExternalId());
        assertThat(services.get(0).get("archived"), is(true));
        assertThat(services.get(0).get("archived_date"), is(notNullValue()));

        serviceRoles = databaseHelper.findServiceRoleForUser(user.getId());
        assertTrue(serviceRoles.isEmpty());

        Map<String, Object> userAttributes = databaseHelper.findUser(user.getId()).get(0);
        assertThat(userAttributes.get("external_id").toString(), is(user.getExternalId()));

        wireMockExtension.verify(getRequestedFor(urlPathEqualTo("/v1/transaction"))
                .withQueryParam("account_id", equalTo(service.getGatewayAccountIds().get(0)))
                .withQueryParam("display_size", equalTo("1"))
        );
    }

    @Test
    void shouldNotArchiveServicesWithTransactionsWithInConfiguredNumberOfDays() throws JsonProcessingException {
        Service service = serviceDbFixture(databaseHelper)
                .withCreatedDate(now.minusYears(8))
                .withGatewayAccountIds(valueOf(nextInt()))
                .insertService();

        Map<String, Object> serviceAttributes = databaseHelper.findServiceByExternalId(service.getExternalId()).get(0);
        assertThat(serviceAttributes.get("external_id"), is(service.getExternalId()));

        LedgerTransaction ledgerTransaction1 = aLedgerTransactionFixture()
                .withCreatedDate(now.minusDays(10))
                .build();
        LedgerSearchTransactionsResponse searchTransactionsResponse = aLedgerSearchTransactionsResponseFixture()
                .withTransactionList(List.of(ledgerTransaction1))
                .build();

        ledgerStub.returnLedgerTransactionsForSearch(service.getGatewayAccountIds().get(0), searchTransactionsResponse);

        givenSetup().when()
                .contentType(JSON)
                .accept(JSON)
                .post("v1/tasks/expunge-and-archive-historical-data")
                .then()
                .statusCode(200);

        serviceAttributes = databaseHelper.findServiceByExternalId(service.getExternalId()).get(0);
        assertThat(serviceAttributes.get("archived"), is(false));

        wireMockExtension.verify(getRequestedFor(urlPathEqualTo("/v1/transaction"))
                .withQueryParam("account_id", equalTo(service.getGatewayAccountIds().get(0)))
                .withQueryParam("display_size", equalTo("1"))
        );
    }

    @Test
    void shouldSet_FirstCheckedForArchivalDate_And_SkipCheckingForArchivalUntilDate_WhenServicesAreNotArchived() throws JsonProcessingException {
        Service serviceWithoutCreatedDateOrTransactions = serviceDbFixture(databaseHelper)
                .withCreatedDate(null)
                .withGatewayAccountIds(valueOf(nextInt()))
                .insertService();
        Service serviceWithoutCreatedDateAndSomeTransactions = serviceDbFixture(databaseHelper)
                .withCreatedDate(null)
                .withGatewayAccountIds(valueOf(nextInt()))
                .insertService();

        LedgerTransaction ledgerTransaction = aLedgerTransactionFixture()
                .withCreatedDate(now.minusDays(4))
                .build();
        LedgerSearchTransactionsResponse searchTransactionsResponse = aLedgerSearchTransactionsResponseFixture()
                .withTransactionList(List.of(ledgerTransaction))
                .build();
        LedgerSearchTransactionsResponse searchTransactionsResponseWitNoResults = aLedgerSearchTransactionsResponseFixture()
                .withTransactionList(List.of(ledgerTransaction))
                .build();

        ledgerStub.returnLedgerTransactionsForSearch(serviceWithoutCreatedDateOrTransactions.getGatewayAccountIds().get(0), searchTransactionsResponseWitNoResults);
        ledgerStub.returnLedgerTransactionsForSearch(serviceWithoutCreatedDateAndSomeTransactions.getGatewayAccountIds().get(0), searchTransactionsResponse);

        givenSetup().when()
                .contentType(JSON)
                .accept(JSON)
                .post("v1/tasks/expunge-and-archive-historical-data")
                .then()
                .statusCode(200);

        Map<String, Object> serviceAttributes = databaseHelper.findServiceByExternalId(serviceWithoutCreatedDateOrTransactions.getExternalId()).get(0);
        assertServiceNotArchived(serviceAttributes);

        serviceAttributes = databaseHelper.findServiceByExternalId(serviceWithoutCreatedDateAndSomeTransactions.getExternalId()).get(0);
        assertServiceNotArchived(serviceAttributes);
    }

    private void assertServiceNotArchived(Map<String, Object> serviceAttributes) {
        assertThat(serviceAttributes.get("archived"), is(false));
        assertThat(serviceAttributes.get("first_checked_for_archival_date"), is(notNullValue()));
        assertThat(serviceAttributes.get("skip_checking_for_archival_until_date"), is(notNullValue()));
    }

    private User insertUser(ZonedDateTime createdDate, ZonedDateTime lastLoggedInAt) {
        return userDbFixture(databaseHelper)
                .withCreatedAt(createdDate)
                .withLastLoggedInAt(lastLoggedInAt)
                .insertUser();
    }

    private void assertUserNotExists(Integer... userIds) {
        Arrays.stream(userIds)
                .forEach(userId -> {
                    List<Map<String, Object>> users = databaseHelper.findUser(userId);
                    assertTrue(users.isEmpty());
                });
    }

    private void assertUserExists(Integer... userIds) {
        Arrays.stream(userIds)
                .forEach(userId -> {
                    Map<String, Object> userAttributes = databaseHelper.findUser(userId).get(0);
                    assertThat(userAttributes.get("id"), is(userId));
                });
    }

    private void assertInvitesExist(String... codes) {
        Arrays.stream(codes)
                .forEach(code -> {
                    Optional<Map<String, Object>> inviteAttributes = databaseHelper.findInviteByCode(code);
                    assertTrue(inviteAttributes.isPresent());
                    assertThat(inviteAttributes.get().get("code"), is(code));
                });
    }

    private String insertInvite(Integer userId, ZonedDateTime date) {
        return inviteDbFixture(databaseHelper)
                .withDate(date)
                .withSenderId(userId)
                .insertInviteToAddUserToService();
    }

    private ForgottenPasswordDbFixture insertForgottenPassword(Integer userId, ZonedDateTime createdDate) {
        return aForgottenPasswordDbFixture()
                .withDatabaseTestHelper(databaseHelper)
                .withUserId(userId)
                .withCreatedAt(createdDate)
                .insert();
    }

    private void assertForgottenPasswordsExist(Object... ids) {
        Arrays.stream(ids)
                .forEach(forgottenPasswordId -> {
                    Map<String, Object> forgottenPasswordAttributes = databaseHelper.findForgottenPasswordById((Integer) forgottenPasswordId).get(0);
                    assertThat(forgottenPasswordAttributes.get("id"), is(forgottenPasswordId));
                });
    }
}
