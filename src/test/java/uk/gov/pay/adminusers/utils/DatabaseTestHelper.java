package uk.gov.pay.adminusers.utils;

import org.skife.jdbi.v2.DBI;

import java.util.List;
import java.util.Map;

public class DatabaseTestHelper {

    private DBI jdbi;

    public DatabaseTestHelper(DBI jdbi) {
        this.jdbi = jdbi;
    }

    public List<Map<String, Object>> findUser(long userId) {
        List<Map<String, Object>> ret = jdbi.withHandle(h ->
                h.createQuery("SELECT id, username, password, email, otp_key, telephone_number, gateway_account_id, disabled, login_counter " +
                        "FROM users " +
                        "WHERE id = :userId")
                        .bind("userId", userId)
                        .list());
        return ret;
    }

}
