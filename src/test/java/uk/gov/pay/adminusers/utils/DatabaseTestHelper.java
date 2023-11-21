package uk.gov.pay.adminusers.utils;

import org.jdbi.v3.core.Jdbi;
import org.postgresql.util.PGobject;
import uk.gov.pay.adminusers.model.MerchantDetails;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.CustomBrandingConverter;
import uk.gov.pay.adminusers.persistence.entity.MerchantDetailsEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.service.ServiceNameEntity;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.sql.Timestamp.from;
import static java.sql.Types.OTHER;

public class DatabaseTestHelper {

    private Jdbi jdbi;

    public DatabaseTestHelper(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<Map<String, Object>> findUserByExternalId(String externalId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT id, external_id, password, email, otp_key, telephone_number, disabled, login_counter, \"createdAt\", \"updatedAt\", session_version " +
                        "FROM users " +
                        "WHERE external_id = :externalId")
                        .bind("externalId", externalId)
                        .mapToMap().list());
    }

    public List<Map<String, Object>> findUserByEmail(String email) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT id, external_id, password, email, otp_key, telephone_number, disabled, login_counter, \"createdAt\", \"updatedAt\", session_version " +
                                "FROM users " +
                                "WHERE email = :email")
                        .bind("email", email)
                        .mapToMap().list());
    }
    public List<Map<String, Object>> findUser(long userId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT id, external_id, password, email, otp_key, telephone_number, disabled, login_counter, \"createdAt\", \"updatedAt\", session_version " +
                        "FROM users " +
                        "WHERE id = :userId")
                        .bind("userId", userId)
                        .mapToMap().list());
    }

    public List<Map<String, Object>> findServiceRoleForUser(long userId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT r.id, r.name, r.description, ur.service_id " +
                        "FROM roles r INNER JOIN user_services_roles ur " +
                        "ON ur.user_id = :userId AND ur.role_id = r.id")
                        .bind("userId", userId)
                        .mapToMap().list());
    }

    public List<Map<String, Object>> findForgottenPasswordById(Integer forgottenPasswordId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT id, date, code, \"userId\" " +
                        "FROM forgotten_passwords " +
                        "WHERE id = :id")
                        .bind("id", forgottenPasswordId)
                        .mapToMap().list());
    }

    public List<Map<String, Object>> findInviteById(Integer inviteId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT id, sender_id, date, code, email, role_id, service_id, otp_key, telephone_number, disabled, login_counter " +
                        "FROM invites " +
                        "WHERE id = :id")
                        .bind("id", inviteId)
                        .mapToMap().list());
    }

    public DatabaseTestHelper updateLoginCount(String email, int loginCount) {
        jdbi.withHandle(handle ->
                handle
                        .createUpdate("UPDATE users SET login_counter = :loginCount " +
                                "WHERE email = :email")
                        .bind("loginCount", loginCount)
                        .bind("email", email)
                        .execute()
        );
        return this;
    }

    public DatabaseTestHelper updateProvisionalOtpKey(String email, String provisionalOtpKey) {
        jdbi.withHandle(handle ->
                handle
                        .createUpdate("UPDATE users SET provisional_otp_key = :provisionalOtpKey, " +
                                "provisional_otp_key_created_at = NOW() " +
                                "WHERE email = :email")
                        .bind("provisionalOtpKey", provisionalOtpKey)
                        .bind("email", email)
                        .execute()
        );
        return this;
    }

    public DatabaseTestHelper add(User user) {
        Timestamp now = from(ZonedDateTime.now(ZoneId.of("UTC")).toInstant());
        jdbi.withHandle(handle ->
                handle
                        .createUpdate("INSERT INTO users(" +
                                "id, external_id, password, email, otp_key, telephone_number, " +
                                "second_factor, disabled, login_counter, version, " +
                                "\"createdAt\", \"updatedAt\", session_version, provisional_otp_key, last_logged_in_at) " +
                                "VALUES (:id, :externalId, :password, :email, :otpKey, :telephoneNumber, " +
                                ":secondFactor, :disabled, :loginCounter, :version, :createdAt, :updatedAt, :session_version, :provisionalOtpKey, :lastLoggedInAt)")
                        .bind("id", user.getId())
                        .bind("externalId", user.getExternalId())
                        .bind("password", user.getPassword())
                        .bind("email", user.getEmail())
                        .bind("otpKey", user.getOtpKey())
                        .bind("telephoneNumber", user.getTelephoneNumber())
                        .bind("secondFactor", user.getSecondFactor().toString())
                        .bind("disabled", user.isDisabled())
                        .bind("loginCounter", user.getLoginCounter())
                        .bind("version", 0)
                        .bind("session_version", user.getSessionVersion())
                        .bind("createdAt", user.getCreatedAt() == null ? now : user.getCreatedAt())
                        .bind("updatedAt", now)
                        .bind("provisionalOtpKey", user.getProvisionalOtpKey())
                        .bind("lastLoggedInAt", user.getLastLoggedInAt())
                        .execute()
        );
        return this;
    }

    //inserting if not exist, just to be safe for fixed value inserts like Admin role
    public DatabaseTestHelper add(Role role) {
        jdbi.withHandle(handle ->
                handle
                        .createUpdate("INSERT INTO roles(id, name, description) " +
                                "SELECT :id, :name, :description " +
                                "WHERE NOT EXISTS (SELECT id FROM roles WHERE id = :id) " +
                                "RETURNING id")
                        .bind("id", role.getId())
                        .bind("name", role.getName())
                        .bind("description", role.getDescription())
                        .execute()
        );
        role.getPermissions().forEach(permission -> jdbi.withHandle(handle ->
                handle.createUpdate("INSERT INTO role_permission(role_id, permission_id) VALUES (:roleId, :permissionId)")
                        .bind("roleId", role.getId())
                        .bind("permissionId", permission.getId())
                        .execute()
        ));
        return this;
    }

    public DatabaseTestHelper add(Permission permission) {
        jdbi.withHandle(handle ->
                handle
                        .createUpdate("INSERT INTO permissions(id, name, description) " +
                                "VALUES (:id, :name, :description)")
                        .bind("id", permission.getId())
                        .bind("name", permission.getName())
                        .bind("description", permission.getDescription())
                        .execute()
        );
        return this;
    }

    public DatabaseTestHelper insertForgottenPassword(Integer id, ZonedDateTime date, String code, Integer userId, ZonedDateTime createdAt) {
        jdbi.withHandle(handle ->
                handle
                        .createUpdate("INSERT INTO forgotten_passwords(id, date, code, \"userId\", \"createdAt\") " +
                                "VALUES (:id, :date, :code, :userId, :createdAt)")
                        .bind("id", id)
                        .bind("date", from(date.toInstant()))
                        .bind("code", code)
                        .bind("userId", userId)
                        .bind("createdAt", from(createdAt.toInstant()))
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
                        .mapToMap().list());
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
            return handle.createUpdate("INSERT INTO services(" +
                    "id, custom_branding, " +
                    "merchant_name, merchant_telephone_number, merchant_address_line1, merchant_address_line2, merchant_address_city, " +
                    "merchant_address_postcode, merchant_address_country, merchant_email, merchant_url, external_id, experimental_features_enabled, takes_payments_over_phone, created_date) " +
                    "VALUES (:id, :customBranding, :merchantName, :merchantTelephoneNumber, :merchantAddressLine1, :merchantAddressLine2, " +
                    ":merchantAddressCity, :merchantAddressPostcode, :merchantAddressCountry, :merchantEmail, :merchantUrl, :externalId, :experimentalFeaturesEnabled, :takesPaymentsOverPhone, :createdDate)")
                    .bind("id", service.getId())
                    .bindBySqlType("customBranding", customBranding, OTHER)
                    .bind("merchantName", merchantDetails.getName())
                    .bind("merchantTelephoneNumber", merchantDetails.getTelephoneNumber())
                    .bind("merchantAddressLine1", merchantDetails.getAddressLine1())
                    .bind("merchantAddressLine2", merchantDetails.getAddressLine2())
                    .bind("merchantAddressCity", merchantDetails.getAddressCity())
                    .bind("merchantAddressPostcode", merchantDetails.getAddressPostcode())
                    .bind("merchantAddressCountry", merchantDetails.getAddressCountry())
                    .bind("merchantEmail", merchantDetails.getEmail())
                    .bind("merchantUrl", merchantDetails.getUrl())
                    .bind("externalId", service.getExternalId())
                    .bind("experimentalFeaturesEnabled", service.isExperimentalFeaturesEnabled())
                    .bind("takesPaymentsOverPhone", service.isTakesPaymentsOverPhone())
                    .bind("createdDate", service.getCreatedDate()!=null? Timestamp.from(service.getCreatedDate().toInstant()): null)
                    .execute();
        });

        addServiceName(ServiceNameEntity.from(SupportedLanguage.ENGLISH, service.getName()), service.getId());
        
        for (String gatewayAccountId : gatewayAccountIds) {
            jdbi.withHandle(handle ->
                    handle.createUpdate("INSERT INTO service_gateway_accounts(service_id, gateway_account_id) VALUES (:serviceId, :gatewayAccountId)")
                            .bind("serviceId", service.getId())
                            .bind("gatewayAccountId", gatewayAccountId)
                            .execute()
            );
        }
        return this;
    }

    public DatabaseTestHelper addUserServiceRole(Integer userId, Integer serviceId, Integer roleId) {
        jdbi.withHandle(handle -> handle
                .createUpdate("INSERT INTO user_services_roles(user_id, service_id, role_id) VALUES(:userId, :serviceId, :roleId)")
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
                        .createUpdate("INSERT INTO invites(id, sender_id, service_id, role_id, email, code, otp_key, date, expiry_date, telephone_number, password, disabled, login_counter) " +
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
                        .createUpdate("INSERT INTO invites(id, sender_id, role_id, email, code, otp_key, date, expiry_date, telephone_number, password, disabled, login_counter) " +
                                "VALUES (:id, :senderId, :roleId, :email, :code, :otpKey, :date, :expiryDate, :telephoneNumber, :password, :disabled, :loginCounter)")
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
                        .execute()
        );
        return this;
    }

    public Optional<Map<String, Object>> findInviteByCode(String code) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT id, sender_id, service_id, role_id, email, code, otp_key, date, telephone_number, disabled, login_counter, password FROM invites " +
                        "WHERE code = :code")
                        .bind("code", code)
                        .mapToMap().findOne());
    }

    public List<Map<String, Object>> findServiceByExternalId(String serviceExternalId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT * FROM services " +
                        "WHERE external_id = :external_id")
                        .bind("external_id", serviceExternalId)
                        .mapToMap().list());
    }

    public List<Map<String, Object>> findServiceNameByServiceId(Integer serviceId) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT * FROM service_names WHERE service_id = :serviceId")
                        .bind("serviceId", serviceId)
                        .mapToMap().list());
    }

    public List<Map<String, Object>> findStripeAgreementById(int id) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM stripe_agreements WHERE id = :id")
                        .bind("id", id)
                        .mapToMap().list());
    }
    
    public List<Map<String, Object>> findGovUkPayAgreementEntity(Integer serviceId) {
        return jdbi.withHandle(handle -> 
                handle.createQuery("SELECT * FROM govuk_pay_agreements WHERE service_id = :id")
                .bind("id", serviceId)
                        .mapToMap().list());
    }
    
    public DatabaseTestHelper insertGovUkPayAgreementEntity(int serviceId, String email, ZonedDateTime agreementTime) {
        jdbi.withHandle(handle -> handle.createUpdate("INSERT INTO govuk_pay_agreements(service_id, agreement_time, email) VALUES (:serviceId, :agreementTime, :email)")
                .bind("serviceId", serviceId)
                .bind("email", email)
                .bind("agreementTime", from(agreementTime.toInstant()))
                .execute());
        return this;
    }

    public DatabaseTestHelper insertStripeAgreementEntity(int serviceId, ZonedDateTime agreementTime, String ipAddress) {
        jdbi.withHandle(handle -> handle
                .createUpdate("INSERT INTO stripe_agreements(service_id, agreement_time, ip_address) VALUES (:serviceId, :agreementTime, :ipAddress)")
                .bind("serviceId", serviceId)
                .bind("agreementTime", from(agreementTime.toInstant()))
                .bind("ipAddress", ipAddress)
                .execute());
        return this;
    }

    private DatabaseTestHelper addServiceName(ServiceNameEntity entity, Integer serviceId) {
        jdbi.withHandle(handle -> handle
                .createUpdate("INSERT INTO service_names(service_id, language, name) VALUES (:serviceId, :language, :name)")
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

            return handle.createUpdate("INSERT INTO services(" +
                    "id, custom_branding, " +
                    "merchant_name, merchant_telephone_number, merchant_address_line1, merchant_address_line2, merchant_address_city, " +
                    "merchant_address_postcode, merchant_address_country, merchant_email, merchant_url, external_id, redirect_to_service_immediately_on_terminal_state, " +
                    "current_go_live_stage, experimental_features_enabled, current_psp_test_account_stage, created_date, archived, first_checked_for_archival_date, skip_checking_for_archival_until_date) " +
                    "VALUES (:id, :customBranding, :merchantName, :merchantTelephoneNumber, :merchantAddressLine1, :merchantAddressLine2, " +
                    ":merchantAddressCity, :merchantAddressPostcode, :merchantAddressCountry, :merchantEmail, :merchantUrl, :externalId, :redirectToServiceImmediatelyOnTerminalState, " +
                    ":currentGoLiveStage, :experimentalFeaturesEnabled, :pspTestAccountStage, :createdDate, :archived, :firstCheckedForArchivalDate, :skipCheckingForArchivalUntilDate)")
                    .bind("id", serviceEntity.getId())
                    .bindBySqlType("customBranding", customBranding, OTHER)
                    .bind("merchantName", merchantDetails.getName())
                    .bind("merchantTelephoneNumber", merchantDetails.getTelephoneNumber())
                    .bind("merchantAddressLine1", merchantDetails.getAddressLine1())
                    .bind("merchantAddressLine2", merchantDetails.getAddressLine2())
                    .bind("merchantAddressCity", merchantDetails.getAddressCity())
                    .bind("merchantAddressPostcode", merchantDetails.getAddressPostcode())
                    .bind("merchantAddressCountry", merchantDetails.getAddressCountryCode())
                    .bind("merchantEmail", merchantDetails.getEmail())
                    .bind("merchantUrl", merchantDetails.getUrl())
                    .bind("externalId", serviceEntity.getExternalId())
                    .bind("redirectToServiceImmediatelyOnTerminalState", serviceEntity.isRedirectToServiceImmediatelyOnTerminalState())
                    .bind("currentGoLiveStage", serviceEntity.getCurrentGoLiveStage())
                    .bind("experimentalFeaturesEnabled", serviceEntity.isExperimentalFeaturesEnabled())
                    .bind("pspTestAccountStage", serviceEntity.getCurrentPspTestAccountStage())
                    .bind("createdDate", serviceEntity.getCreatedDate())
                    .bind("archived", serviceEntity.isArchived())
                    .bind("firstCheckedForArchivalDate", serviceEntity.getFirstCheckedForArchivalDate())
                    .bind("skipCheckingForArchivalUntilDate", serviceEntity.getSkipCheckingForArchivalUntilDate())
                    .execute();
        });
        serviceEntity.getGatewayAccountIds().forEach(gatewayAccount ->
                jdbi.withHandle(handle ->
                        handle.createUpdate("INSERT INTO service_gateway_accounts(service_id, gateway_account_id) VALUES (:serviceId, :gatewayAccountId)")
                                .bind("serviceId", serviceEntity.getId())
                                .bind("gatewayAccountId", gatewayAccount.getGatewayAccountId())
                                .execute()
                ));
        serviceEntity.getServiceNames().values().forEach((name) -> addServiceName(name, serviceEntity.getId()));
        return this;
    }

    public void truncateAllData() {
        jdbi.withHandle(handle -> handle.createUpdate("TRUNCATE TABLE users CASCADE").execute());
        jdbi.withHandle(handle -> handle.createUpdate("TRUNCATE TABLE services CASCADE").execute());
    }
}
