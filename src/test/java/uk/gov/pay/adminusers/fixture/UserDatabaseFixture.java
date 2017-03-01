package uk.gov.pay.adminusers.fixture;

import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;

public class UserDatabaseFixture {

    private final DatabaseTestHelper database;
    private int userId = nextInt();
    private String password = randomAlphanumeric(20);
    private int serviceId = nextInt();

    public UserDatabaseFixture(DatabaseTestHelper database) {
        this.database = database;
    }

    public static UserDatabaseFixture aUserDatabaseFixture(DatabaseTestHelper database) {
        return new UserDatabaseFixture(database);
    }

    public UserDatabaseFixture aUser() {
        return this;
    }

    public UserDatabaseFixture withId(int userId) {
        this.userId = userId;
        return this;
    }

    public void build() {
        String username = randomAlphabetic(20);
        database.addUser(userId, username, password, username + "@example.com", "784rh", "8948924", "1", false, 0, 0);
        database.addService(serviceId, randomNumeric(2));
    }

    public UserDatabaseFixture withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserDatabaseFixture withService(int serviceId) {
        this.serviceId = serviceId;
        return this;
    }
}
