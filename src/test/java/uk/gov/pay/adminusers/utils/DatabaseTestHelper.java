package uk.gov.pay.adminusers.utils;

import org.skife.jdbi.v2.DBI;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
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

    public List<Map<String, Object>> findRolesForUser(long userId) {
        List<Map<String, Object>> ret = jdbi.withHandle(h ->
                h.createQuery("SELECT r.id, r.name, r.description " +
                        "FROM roles r INNER JOIN user_role ur " +
                        "ON ur.user_id = :userId AND ur.role_id=r.id")
                        .bind("userId", userId)
                        .list());
        return ret;
    }

    public DatabaseTestHelper addUser(User user) {
        jdbi.withHandle(handle ->
                handle
                        .createStatement("INSERT INTO users(id, username, password, email, otp_key, telephone_number, gateway_account_id, disabled, login_counter, version) " +
                                "VALUES (:id, :username, :password, :email, :otpKey, :telephoneNumber, :gatewayAccountId, :disabled, :loginCounter, :version)")
                        .bind("id", user.getId())
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
        user.getRoles().forEach(userRole -> {
            jdbi.withHandle(handle ->
                    handle.createStatement("INSERT INTO user_role(user_id, role_id) VALUES (:userId, :roleId)")
                            .bind("userId", user.getId())
                            .bind("roleId", userRole.getId())
                            .execute()
            );
        });
        return this;
    }

    public DatabaseTestHelper addRole(Role role) {
        jdbi.withHandle(handle ->
                handle
                        .createStatement("INSERT INTO roles(id, name, description) " +
                                "VALUES (:id, :name, :description)")
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

    public DatabaseTestHelper addPermission(Permission permission) {
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
}
