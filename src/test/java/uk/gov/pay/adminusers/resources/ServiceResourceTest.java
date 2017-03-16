package uk.gov.pay.adminusers.resources;

import org.hamcrest.core.Is;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.User;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.*;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.model.Role.role;

public class ServiceResourceTest extends IntegrationTest {

    @Test
    public void shouldReturnListOfAllUsersWithRolesForAGivenServiceOrderedByRoleName() {

        int serviceId1 = nextInt();
        int serviceId2 = nextInt();
        int roleId1 = nextInt();
        int roleId2 = nextInt();
        String username1 = "user-1-roleB-" + randomUUID().toString();
        String username2 = "user-2-roleA-" + randomUUID().toString();
        String username3 = "user-3-roleA-" + randomUUID().toString();
        String username4 = "user-4-roleB-" + randomUUID().toString();
        Integer user1Id = randomInt();
        User user1 = User.from(user1Id, username1, format("%s-password", username1), format("%s@email.com", username1), asList("1"), "784rh", "8948924");
        User user2 = User.from(user1Id + 1, username2, format("%s-password", username2), format("%s@email.com", username2), asList("1"), "784rh", "8948924");
        User user3 = User.from(user1Id + 2, username3, format("%s-password", username3), format("%s@email.com", username3), asList("1"), "784rh", "8948924");
        User user4 = User.from(user1Id + 3, username4, format("%s-password", username4), format("%s@email.com", username4), asList("1"), "784rh", "8948924");

        String gatewayAccountId1 = valueOf(nextInt());
        String gatewayAccountId2 = valueOf(nextInt());
        Role role1 = role(roleId1, "roleB", "role-desc-B");
        Role role2 = role(roleId2, "roleA", "role-desc-A");
        Permission permission1 = Permission.permission(nextInt(), "perm1-name", "perm1-desc");
        Permission permission2 = Permission.permission(nextInt(), "perm2-name", "perm2-desc");
        role1.setPermissions(newArrayList(permission1));
        role2.setPermissions(newArrayList(permission2));

        databaseTestHelper.addService(serviceId1, gatewayAccountId1);
        databaseTestHelper.addService(serviceId2, gatewayAccountId2);
        databaseTestHelper.add(permission1);
        databaseTestHelper.add(permission2);
        databaseTestHelper.add(role1);
        databaseTestHelper.add(role2);
        databaseTestHelper.add(user1, serviceId1, roleId1);
        databaseTestHelper.add(user2, serviceId1, roleId2);
        databaseTestHelper.add(user3, serviceId1, roleId2);
        databaseTestHelper.add(user4, serviceId2, roleId1);

        givenSetup()
                .when()
                .accept(JSON)
                .get(String.format("/v1/api/services/%s/users", serviceId1))
                .then()
                .statusCode(200)
                .body("$", hasSize(3))
                .body("[0].username", is(username3))
                .body("[1].username", is(username2))
                .body("[2].username", is(username1));
    }
}
