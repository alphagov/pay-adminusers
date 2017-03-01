package uk.gov.pay.adminusers.fixture;

import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

public class ServiceDatabaseFixture {

    private final DatabaseTestHelper database;

    public ServiceDatabaseFixture(DatabaseTestHelper database) {
        this.database = database;
    }

    public ServiceDatabaseFixture aService(){
        return this;
    }
}
