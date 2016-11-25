package uk.gov.pay.adminusers.utils;

import org.skife.jdbi.v2.DBI;

public class DatabaseTestHelper {

    private DBI jdbi;

    public DatabaseTestHelper(DBI jdbi) {
        this.jdbi = jdbi;
    }

}
