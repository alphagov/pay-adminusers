package uk.gov.pay.adminusers.utils;

import org.skife.jdbi.v2.DBI;
import uk.gov.pay.adminusers.model.User;

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

    public void addUser(User user) {
        jdbi.withHandle(handle ->
                handle
                        .createStatement("INSERT INTO users(username, password, email, otp_key, telephone_number, gateway_account_id, disabled, login_counter, version) " +
                                "VALUES (:username, :password, :email, :otpKey, :telephoneNumber, :gatewayAccountId, :disabled, :loginCounter, :version)")
                        .bind("username", user.getUsername())
                        .bind("password", user.getPassword())
                        .bind("email", user.getEmail())
                        .bind("otpKey", user.getOtpKey())
                        .bind("telephoneNumber", user.getTelephoneNumber())
                        .bind("gatewayAccountId", user.getGatewayAccountId())
                        .bind("disabled", user.getDisabled())
                        .bind("loginCounter", user.getLoginCount())
                        .bind("version", 0)
                        .execute()
        );
    }
}
