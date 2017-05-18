package uk.gov.pay.adminusers.utils;

import org.skife.jdbi.v2.DBI;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.User;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static java.sql.Timestamp.from;

public class DatabaseTestHelper {

    private DBI jdbi;

    public DatabaseTestHelper(DBI jdbi) {
        this.jdbi = jdbi;
    }

    public List<Map<String, Object>> findUserByExternalId(String externalId) {
        List<Map<String, Object>> ret = jdbi.withHandle(h ->
                h.createQuery("SELECT id, external_id, username, password, email, otp_key, telephone_number, disabled, login_counter, \"createdAt\", \"updatedAt\", session_version " +
                        "FROM users " +
                        "WHERE external_id = :externalId")
                        .bind("externalId", externalId)
                        .list());
        return ret;
    }

    public List<Map<String, Object>> findUserByUsername(String username) {
        List<Map<String, Object>> ret = jdbi.withHandle(h ->
                h.createQuery("SELECT id, external_id, username, password, email, otp_key, telephone_number, disabled, login_counter, \"createdAt\", \"updatedAt\", session_version " +
                        "FROM users " +
                        "WHERE username = :username")
                        .bind("username", username)
                        .list());
        return ret;
    }

    public List<Map<String, Object>> findUser(long userId) {
        List<Map<String, Object>> ret = jdbi.withHandle(h ->
                h.createQuery("SELECT id, external_id, username, password, email, otp_key, telephone_number, disabled, login_counter, \"createdAt\", \"updatedAt\", session_version " +
                        "FROM users " +
                        "WHERE id = :userId")
                        .bind("userId", userId)
                        .list());
        return ret;
    }

    public List<Map<String, Object>> findServiceRoleForUser(long userId) {
        List<Map<String, Object>> ret = jdbi.withHandle(h ->
                h.createQuery("SELECT r.id, r.name, r.description, ur.service_id " +
                        "FROM roles r INNER JOIN user_services_roles ur " +
                        "ON ur.user_id = :userId AND ur.role_id = r.id")
                        .bind("userId", userId)
                        .list());
        return ret;
    }

    public List<Map<String, Object>> findForgottenPasswordById(Integer forgottenPasswordId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT id, date, code, \"userId\" " +
                        "FROM forgotten_passwords " +
                        "WHERE id = :id")
                        .bind("id", forgottenPasswordId)
                        .list());
    }

    public List<Map<String, Object>> findInviteById(Integer inviteId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT id, sender_id, date, code, email, role_id, service_id, otp_key, telephone_number, disabled, login_counter " +
                        "FROM invites " +
                        "WHERE id = :id")
                        .bind("id", inviteId)
                        .list());
    }

    public DatabaseTestHelper updateLoginCount(String username, int loginCount) {
        jdbi.withHandle(handle ->
                handle
                        .createStatement("UPDATE users SET login_counter = :loginCount " +
                                "WHERE username = :username")
                        .bind("loginCount", loginCount)
                        .bind("username", username)
                        .execute()
        );
        return this;
    }

    public DatabaseTestHelper add(User user, int roleId) {
        Timestamp now = from(ZonedDateTime.now(ZoneId.of("UTC")).toInstant());
        jdbi.withHandle(handle ->
                handle
                        .createStatement("INSERT INTO users(" +
                                "id, external_id, username, password, email, otp_key, telephone_number, disabled, login_counter, version, \"createdAt\", \"updatedAt\", session_version) " +
                                "VALUES (:id, :externalId, :username, :password, :email, :otpKey, :telephoneNumber, :disabled, :loginCounter, :version, :createdAt, :updatedAt, :session_version)")
                        .bind("id", user.getId())
                        .bind("externalId", user.getExternalId())
                        .bind("username", user.getUsername())
                        .bind("password", user.getPassword())
                        .bind("email", user.getEmail())
                        .bind("otpKey", user.getOtpKey())
                        .bind("telephoneNumber", user.getTelephoneNumber())
                        .bind("disabled", user.isDisabled())
                        .bind("loginCounter", user.getLoginCounter())
                        .bind("version", 0)
                        .bind("session_version", user.getSessionVersion())
                        .bind("createdAt", now)
                        .bind("updatedAt", now)
                        .execute()
        );

        jdbi.withHandle(handle -> handle
                .createStatement("INSERT INTO user_services_roleS(user_id, service_id, role_id) VALUES(:userId, :serviceId, :roleId)")
                .bind("userId", user.getId())
                .bind("serviceId", Integer.valueOf(user.getServiceIds().get(0)))
                .bind("roleId", roleId)
                .execute());

        return this;
    }

    //inserting if not exist, just to be safe for fixed value inserts like Admin role
    public DatabaseTestHelper add(Role role) {
        jdbi.withHandle(handle ->
                handle
                        .createStatement("INSERT INTO roles(id, name, description) " +
                                "SELECT :id, :name, :description " +
                                "WHERE NOT EXISTS (SELECT id FROM roles WHERE id = :id) " +
                                "RETURNING id")
                        .bind("id", role.getId())
                        .bind("name", role.getName())
                        .bind("description", role.getDescription())
                        .execute()
        );
        role.getPermissions().forEach(permission -> {
            jdbi.withHandle(handle ->
                    handle.createStatement("INSERT INTO role_permission(role_id, permission_id) VALUES (:roleId, :permissionId)")
                            .bind("roleId", role.getId())
                            .bind("permissionId", permission.getId())
                            .execute()
            );
        });
        return this;
    }

    public DatabaseTestHelper add(Permission permission) {
        jdbi.withHandle(handle ->
                handle
                        .createStatement("INSERT INTO permissions(id, name, description) " +
                                "VALUES (:id, :name, :description)")
                        .bind("id", permission.getId())
                        .bind("name", permission.getName())
                        .bind("description", permission.getDescription())
                        .execute()
        );
        return this;
    }

    public DatabaseTestHelper add(ForgottenPassword forgottenPassword, Integer userId) {
        jdbi.withHandle(handle ->
                handle
                        .createStatement("INSERT INTO forgotten_passwords(id, date, code, \"userId\") " +
                                "VALUES (:id, :date, :code, :userId)")
                        .bind("id", forgottenPassword.getId())
                        .bind("date", from(forgottenPassword.getDate().toInstant()))
                        .bind("code", forgottenPassword.getCode())
                        .bind("userId", userId)
                        .execute()
        );
        return this;
    }

    //TODO Remove - This is temporary - WIP PP-1483
    public List<Map<String, Object>> findUserServicesByUserId(Integer userId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT service_id FROM user_services_roles " +
                        "WHERE user_id = :userId")
                        .bind("userId", userId)
                        .list());
    }

    public List<Map<String, Object>> findGatewayAccountsByService(Long serviceId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT gateway_account_id FROM service_gateway_accounts " +
                        "WHERE service_id = :serviceId")
                        .bind("serviceId", serviceId)
                        .list());
    }

    public DatabaseTestHelper addService(int serviceId, String... gatewayAccountIds) {
        jdbi.withHandle(handle ->
                handle
                        .createStatement("INSERT INTO services(id) " +
                                "VALUES (:id)")
                        .bind("id", serviceId)
                        .execute()
        );

        for (String gatewayAccountId : gatewayAccountIds) {
            jdbi.withHandle(handle ->
                    handle.createStatement("INSERT INTO service_gateway_accounts(service_id, gateway_account_id) VALUES (:serviceId, :gatewayAccountId)")
                            .bind("serviceId", serviceId)
                            .bind("gatewayAccountId", gatewayAccountId)
                            .execute()
            );
        }
        return this;
    }

    public DatabaseTestHelper addUserServiceRole(Integer userId, Integer serviceId, Integer roleId) {
        jdbi.withHandle(handle -> handle
                .createStatement("INSERT INTO user_services_roles(user_id, service_id, role_id) VALUES(:userId, :serviceId, :roleId)")
                .bind("userId", userId)
                .bind("serviceId", serviceId)
                .bind("roleId", roleId)
                .execute());
        return this;
    }

    public DatabaseTestHelper addInvite(int id, int senderId, int serviceId, int roleId, String email, String code, String otpKey, ZonedDateTime date, ZonedDateTime expiryDate, String telephoneNumber, String password) {
        jdbi.withHandle(handle ->
                handle
                        .createStatement("INSERT INTO invites(id, sender_id, service_id, role_id, email, code, otp_key, date, expiry_date, telephone_number, password) " +
                                "VALUES (:id, :senderId, :serviceId, :roleId, :email, :code, :otpKey, :date, :expiryDate, :telephoneNumber, :password)")
                        .bind("id", id)
                        .bind("senderId", senderId)
                        .bind("serviceId", serviceId)
                        .bind("roleId", roleId)
                        .bind("email", email)
                        .bind("code", code)
                        .bind("otpKey", otpKey)
                        .bind("telephoneNumber", telephoneNumber)
                        .bind("password", password)
                        .bind("date", from(date.toInstant()))
                        .bind("expiryDate", from(expiryDate.toInstant()))
                        .execute()
        );
        return this;
    }

    public List<Map<String, Object>> findInviteByCode(String code) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT id, sender_id, service_id, role_id, email, code, otp_key, date, telephone_number FROM invites " +
                        "WHERE code = :code")
                        .bind("code", code)
                        .list());
    }
}
