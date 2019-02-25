package uk.gov.pay.adminusers.utils;

import org.postgresql.util.PGobject;
import org.skife.jdbi.v2.DBI;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.MerchantDetails;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.CustomBrandingConverter;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;

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
        return jdbi.withHandle(h ->
                h.createQuery("SELECT id, external_id, username, password, email, otp_key, telephone_number, disabled, login_counter, \"createdAt\", \"updatedAt\", session_version " +
                        "FROM users " +
                        "WHERE external_id = :externalId")
                        .bind("externalId", externalId)
                        .list());
    }

    public List<Map<String, Object>> findUserByUsername(String username) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT id, external_id, username, password, email, otp_key, telephone_number, disabled, login_counter, \"createdAt\", \"updatedAt\", session_version " +
                        "FROM users " +
                        "WHERE username = :username")
                        .bind("username", username)
                        .list());
    }

    public List<Map<String, Object>> findUser(long userId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT id, external_id, username, password, email, otp_key, telephone_number, disabled, login_counter, \"createdAt\", \"updatedAt\", session_version " +
                        "FROM users " +
                        "WHERE id = :userId")
                        .bind("userId", userId)
                        .list());
    }

    public List<Map<String, Object>> findServiceRoleForUser(long userId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT r.id, r.name, r.description, ur.service_id " +
                        "FROM roles r INNER JOIN user_services_roles ur " +
                        "ON ur.user_id = :userId AND ur.role_id = r.id")
                        .bind("userId", userId)
                        .list());
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

    public DatabaseTestHelper updateProvisionalOtpKey(String username, String provisionalOtpKey) {
        jdbi.withHandle(handle ->
                handle
                        .createStatement("UPDATE users SET provisional_otp_key = :provisionalOtpKey, " +
                                "provisional_otp_key_created_at = NOW() " +
                                "WHERE username = :username")
                        .bind("provisionalOtpKey", provisionalOtpKey)
                        .bind("username", username)
                        .execute()
        );
        return this;
    }

    public DatabaseTestHelper add(User user) {
        Timestamp now = from(ZonedDateTime.now(ZoneId.of("UTC")).toInstant());
        jdbi.withHandle(handle ->
                handle
                        .createStatement("INSERT INTO users(" +
                                "id, external_id, username, password, email, otp_key, telephone_number, " +
                                "second_factor, disabled, login_counter, version, " +
                                "\"createdAt\", \"updatedAt\", session_version, provisional_otp_key) " +
                                "VALUES (:id, :externalId, :username, :password, :email, :otpKey, :telephoneNumber, " +
                                ":secondFactor, :disabled, :loginCounter, :version, :createdAt, :updatedAt, :session_version, :provisionalOtpKey)")
                        .bind("id", user.getId())
                        .bind("externalId", user.getExternalId())
                        .bind("username", user.getUsername())
                        .bind("password", user.getPassword())
                        .bind("email", user.getEmail())
                        .bind("otpKey", user.getOtpKey())
                        .bind("telephoneNumber", user.getTelephoneNumber())
                        .bind("secondFactor", user.getSecondFactor().toString())
                        .bind("disabled", user.isDisabled())
                        .bind("loginCounter", user.getLoginCounter())
                        .bind("version", 0)
                        .bind("session_version", user.getSessionVersion())
                        .bind("createdAt", now)
                        .bind("updatedAt", now)
                        .bind("provisionalOtpKey", user.getProvisionalOtpKey())
                        .execute()
        );
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
        role.getPermissions().forEach(permission -> jdbi.withHandle(handle ->
                handle.createStatement("INSERT INTO role_permission(role_id, permission_id) VALUES (:roleId, :permissionId)")
                        .bind("roleId", role.getId())
                        .bind("permissionId", permission.getId())
                        .execute()
        ));
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

    public DatabaseTestHelper addService(Service service, String... gatewayAccountIds) {
        jdbi.withHandle(handle ->
        {
            PGobject customBranding = service.getCustomBranding() == null ? null :
                    new CustomBrandingConverter().convertToDatabaseColumn(service.getCustomBranding());
            MerchantDetails merchantDetails = service.getMerchantDetails();
            if (merchantDetails == null) {
                merchantDetails = new MerchantDetails();
            }
            return handle.createStatement("INSERT INTO services(" +
                    "id, name, custom_branding, " +
                    "merchant_name, merchant_telephone_number, merchant_address_line1, merchant_address_line2, merchant_address_city, " +
                    "merchant_address_postcode, merchant_address_country, merchant_email, external_id) " +
                    "VALUES (:id, :name, :customBranding, :merchantName, :merchantTelephoneNumber, :merchantAddressLine1, :merchantAddressLine2, " +
                    ":merchantAddressCity, :merchantAddressPostcode, :merchantAddressCountry, :merchantEmail, :externalId)")
                    .bind("id", service.getId())
                    .bind("name", service.getName())
                    .bind("customBranding", customBranding)
                    .bind("merchantName", merchantDetails.getName())
                    .bind("merchantTelephoneNumber", merchantDetails.getTelephoneNumber())
                    .bind("merchantAddressLine1", merchantDetails.getAddressLine1())
                    .bind("merchantAddressLine2", merchantDetails.getAddressLine2())
                    .bind("merchantAddressCity", merchantDetails.getAddressCity())
                    .bind("merchantAddressPostcode", merchantDetails.getAddressPostcode())
                    .bind("merchantAddressCountry", merchantDetails.getAddressCountry())
                    .bind("merchantEmail", merchantDetails.getEmail())
                    .bind("externalId", service.getExternalId())
                    .execute();
        });

        for (String gatewayAccountId : gatewayAccountIds) {
            jdbi.withHandle(handle ->
                    handle.createStatement("INSERT INTO service_gateway_accounts(service_id, gateway_account_id) VALUES (:serviceId, :gatewayAccountId)")
                            .bind("serviceId", service.getId())
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

    public DatabaseTestHelper addInvite(int id, int senderId, int serviceId, int roleId,
                                        String email, String code, String otpKey,
                                        ZonedDateTime date, ZonedDateTime expiryDate,
                                        String telephoneNumber, String password,
                                        Boolean disabled,
                                        Integer loginCounter) {
        jdbi.withHandle(handle ->
                handle
                        .createStatement("INSERT INTO invites(id, sender_id, service_id, role_id, email, code, otp_key, date, expiry_date, telephone_number, password, disabled, login_counter) " +
                                "VALUES (:id, :senderId, :serviceId, :roleId, :email, :code, :otpKey, :date, :expiryDate, :telephoneNumber, :password, :disabled, :loginCounter)")
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
                        .bind("disabled", disabled)
                        .bind("loginCounter", loginCounter)
                        .execute()
        );
        return this;
    }

    public DatabaseTestHelper addServiceInvite(int id, int senderId, int roleId,
                                               String email, String code, String otpKey,
                                               ZonedDateTime date, ZonedDateTime expiryDate,
                                               String telephoneNumber, String password,
                                               Boolean disabled,
                                               Integer loginCounter) {
        jdbi.withHandle(handle ->
                handle
                        .createStatement("INSERT INTO invites(id, sender_id, role_id, email, code, otp_key, date, expiry_date, telephone_number, password, disabled, login_counter, type) " +
                                "VALUES (:id, :senderId, :roleId, :email, :code, :otpKey, :date, :expiryDate, :telephoneNumber, :password, :disabled, :loginCounter, :type)")
                        .bind("id", id)
                        .bind("senderId", senderId)
                        .bind("roleId", roleId)
                        .bind("email", email)
                        .bind("code", code)
                        .bind("otpKey", otpKey)
                        .bind("telephoneNumber", telephoneNumber)
                        .bind("password", password)
                        .bind("date", from(date.toInstant()))
                        .bind("expiryDate", from(expiryDate.toInstant()))
                        .bind("disabled", disabled)
                        .bind("loginCounter", loginCounter)
                        .bind("type", "service")
                        .execute()
        );
        return this;
    }

    public List<Map<String, Object>> findInviteByCode(String code) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT id, sender_id, service_id, role_id, email, code, otp_key, date, telephone_number, disabled, login_counter FROM invites " +
                        "WHERE code = :code")
                        .bind("code", code)
                        .list());
    }

    public List<Map<String, Object>> findServiceByExternalId(String serviceExternalId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT * FROM services " +
                        "WHERE external_id = :external_id")
                        .bind("external_id", serviceExternalId)
                        .list());
    }

    public List<Map<String, Object>> findServiceNameByServiceId(Integer serviceId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT * FROM service_names WHERE service_id = :serviceId")
                        .bind("serviceId", serviceId)
                        .list());
    }

    public List<Map<String, Object>> findStripeAgreementById(int id) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM stripe_agreements WHERE id = :id")
                        .bind("id", id)
                        .list());
    }
    
    public List<Map<String, Object>> findGovUkPayAgreementEntity(Integer serviceId) {
        return jdbi.withHandle(handle -> 
                handle.createQuery("SELECT * FROM govuk_pay_agreements WHERE service_id = :id")
                .bind("id", serviceId)
                .list());
    }
    
    public DatabaseTestHelper insertGovUkPayAgreementEntity(int serviceId, String email, ZonedDateTime agreementTime) {
        jdbi.withHandle(handle -> handle.createStatement("INSERT INTO govuk_pay_agreements(service_id, agreement_time, email) VALUES (:serviceId, :agreementTime, :email)")
                .bind("serviceId", serviceId)
                .bind("email", email)
                .bind("agreementTime", from(agreementTime.toInstant()))
                .execute());
        return this;
    }

    public DatabaseTestHelper insertStripeAgreementEntity(int serviceId, ZonedDateTime agreementTime, String ipAddress) {
        jdbi.withHandle(handle -> handle
                .createStatement("INSERT INTO stripe_agreements(service_id, agreement_time, ip_address) VALUES (:serviceId, :agreementTime, :ipAddress)")
                .bind("serviceId", serviceId)
                .bind("agreementTime", from(agreementTime.toInstant()))
                .bind("ipAddress", ipAddress)
                .execute());
        return this;
    }

    private DatabaseTestHelper addServiceName(ServiceNameEntity entity, Integer serviceId) {
        jdbi.withHandle(handle -> handle
                .createStatement("INSERT INTO service_names(id, service_id, language, name) VALUES (:id, :serviceId, :language, :name)")
                .bind("id", entity.getId())
                .bind("serviceId", serviceId)
                .bind("language", entity.getLanguage().toString())
                .bind("name", entity.getName())
                .execute());
        return this;
    }

    public DatabaseTestHelper insertServiceEntity(ServiceEntity serviceEntity) {
        jdbi.withHandle(handle ->
        {
            PGobject customBranding = serviceEntity.getCustomBranding() == null ? null :
                    new CustomBrandingConverter().convertToDatabaseColumn(serviceEntity.getCustomBranding());
            MerchantDetailsEntity merchantDetails = serviceEntity.getMerchantDetailsEntity();

            return handle.createStatement("INSERT INTO services(" +
                    "id, name, custom_branding, " +
                    "merchant_name, merchant_telephone_number, merchant_address_line1, merchant_address_line2, merchant_address_city, " +
                    "merchant_address_postcode, merchant_address_country, merchant_email, external_id, redirect_to_service_immediately_on_terminal_state, current_go_live_stage) " +
                    "VALUES (:id, :name, :customBranding, :merchantName, :merchantTelephoneNumber, :merchantAddressLine1, :merchantAddressLine2, " +
                    ":merchantAddressCity, :merchantAddressPostcode, :merchantAddressCountry, :merchantEmail, :externalId, :redirectToServiceImmediatelyOnTerminalState, :currentGoLiveStage)")
                    .bind("id", serviceEntity.getId())
                    .bind("name", serviceEntity.getName())
                    .bind("customBranding", customBranding)
                    .bind("merchantName", merchantDetails.getName())
                    .bind("merchantTelephoneNumber", merchantDetails.getTelephoneNumber())
                    .bind("merchantAddressLine1", merchantDetails.getAddressLine1())
                    .bind("merchantAddressLine2", merchantDetails.getAddressLine2())
                    .bind("merchantAddressCity", merchantDetails.getAddressCity())
                    .bind("merchantAddressPostcode", merchantDetails.getAddressPostcode())
                    .bind("merchantAddressCountry", merchantDetails.getAddressCountryCode())
                    .bind("merchantEmail", merchantDetails.getEmail())
                    .bind("externalId", serviceEntity.getExternalId())
                    .bind("redirectToServiceImmediatelyOnTerminalState", serviceEntity.isRedirectToServiceImmediatelyOnTerminalState())
                    .bind("currentGoLiveStage", serviceEntity.getCurrentGoLiveStage())
                    .execute();
        });
        serviceEntity.getGatewayAccountIds().forEach(gatewayAccount ->
                jdbi.withHandle(handle ->
                        handle.createStatement("INSERT INTO service_gateway_accounts(service_id, gateway_account_id) VALUES (:serviceId, :gatewayAccountId)")
                                .bind("serviceId", serviceEntity.getId())
                                .bind("gatewayAccountId", gatewayAccount.getGatewayAccountId())
                                .execute()
                ));
        serviceEntity.getServiceNames().values().forEach((name) -> addServiceName(name, serviceEntity.getId()));
        return this;
    }

    public void truncateAllData() {
        jdbi.withHandle(handle -> handle.createStatement("TRUNCATE TABLE users CASCADE").execute());
        jdbi.withHandle(handle -> handle.createStatement("TRUNCATE TABLE services CASCADE").execute());
    }
}
