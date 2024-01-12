package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.model.PatchRequest;
import uk.gov.pay.adminusers.model.SecondFactorMethod;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.utils.telephonenumber.TelephoneNumberUtility;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_DISABLED;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_EMAIL;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_FEATURES;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_SESSION_VERSION;
import static uk.gov.pay.adminusers.model.PatchRequest.PATH_TELEPHONE_NUMBER;
import static uk.gov.pay.adminusers.model.SecondFactorMethod.SMS;

public class UserServices {

    private static Logger logger = LoggerFactory.getLogger(UserServices.class);

    private final UserDao userDao;
    private final PasswordHasher passwordHasher;
    private final LinksBuilder linksBuilder;
    private final Integer loginAttemptCap;
    private final SecondFactorAuthenticator secondFactorAuthenticator;

    @Inject
    public UserServices(UserDao userDao,
                        PasswordHasher passwordHasher,
                        LinksBuilder linksBuilder,
                        @Named("LOGIN_ATTEMPT_CAP") Integer loginAttemptCap,
                        Provider<NotificationService> userNotificationService, 
                        SecondFactorAuthenticator secondFactorAuthenticator, 
                        ServiceFinder serviceFinder) {
        this.userDao = userDao;
        this.passwordHasher = passwordHasher;
        this.linksBuilder = linksBuilder;
        this.loginAttemptCap = loginAttemptCap;
        this.secondFactorAuthenticator = secondFactorAuthenticator;
    }

    @Transactional
    public Optional<User> authenticate(String email, String password) {
        Optional<UserEntity> userEntityOptional = userDao.findByEmail(email);
        logger.debug("Login attempt - email={}", email);
        if (userEntityOptional.isPresent()) {
            UserEntity userEntity = userEntityOptional.get();
            if (passwordHasher.isEqual(password, userEntity.getPassword())) {
                if (!userEntity.isDisabled()) {
                    userEntity.setLoginCounter(0);
                    userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                    userDao.merge(userEntity);
                }

                logger.info("Successful Login - user_id={}", userEntity.getExternalId());
                return Optional.of(linksBuilder.decorate(userEntity.toUser()));
            } else {
                userEntity.setLoginCounter(userEntity.getLoginCounter() + 1);
                userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                userEntity.setDisabled(userEntity.getLoginCounter() >= loginAttemptCap);
                logger.info("Failed login attempt - user_id={}, login_counter={}", userEntity.getExternalId(), userEntity.getLoginCounter());
                userDao.merge(userEntity);
                if (userEntity.isDisabled()) {
                    logger.warn("Account locked due to exceeding {} attempts - user_id={}", loginAttemptCap, userEntity.getExternalId());
                }
                return Optional.empty();
            }
        } else {
            logger.info("Failed login attempt - user_id='Not matched'");
            return Optional.empty();
        }
    }

    public Optional<UserEntity> findUserByExternalId(String externalId) {
        return userDao.findByExternalId(externalId);
    }

    public List<User> findUsersByExternalIds(List<String> externalIds) {
        return userDao.findByExternalIds(externalIds)
                .stream()
                .map(UserEntity::toUser)
                .map(linksBuilder::decorate)
                .collect(toUnmodifiableList());
    }

    public Optional<User> findUserByEmail(String username) {
        Optional<UserEntity> userEntityOptional = userDao.findByEmail(username);
        return userEntityOptional.map(userEntity -> linksBuilder.decorate(userEntity.toUser()));
    }
    
    public Map<String, List<String>> getAdminUserEmailsForGatewayAccountIds(List<String> gatewayAccountIds) {
        Map<String, List<String>> gatewayAccountIdsToAdminEmails = new HashMap<>(userDao.getAdminUserEmailsForGatewayAccountIds(gatewayAccountIds));
        gatewayAccountIds.forEach(gatewayAccountId -> gatewayAccountIdsToAdminEmails.putIfAbsent(gatewayAccountId, List.of()));
        return Map.copyOf(gatewayAccountIdsToAdminEmails);
    }

    @Transactional
    public Optional<User> authenticateSecondFactor(String externalId, int code) {
        logger.debug("OTP attempt - user_id={}", externalId);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        return userDao.findByExternalId(externalId)
                .map(userEntity -> {
                    if (userEntity.isDisabled()) {
                        logger.warn("Failed OTP attempt - user_id={}, login_counter={}. Authenticate Second Factor attempted for a disabled User", userEntity.getExternalId(), userEntity.getLoginCounter());
                        return Optional.<User>empty();
                    }
                    if (secondFactorAuthenticator.authorize(userEntity.getOtpKey(), code)) {
                        userEntity.setLoginCounter(0);
                        userEntity.setUpdatedAt(now);
                        userEntity.setLastLoggedInAt(now);
                        userDao.merge(userEntity);
                        logger.info("Successful OTP. user_id={}", userEntity.getExternalId());
                        return Optional.of(linksBuilder.decorate(userEntity.toUser()));
                    } else {
                        userEntity.setLoginCounter(userEntity.getLoginCounter() + 1);
                        userEntity.setUpdatedAt(now);
                        userEntity.setDisabled(userEntity.getLoginCounter() > loginAttemptCap);
                        userDao.merge(userEntity);
                        if (userEntity.isDisabled()) {
                            logger.warn("Failed OTP attempt - user_id={}, login_counter={}. Invalid second factor in an account currently locked", userEntity.getExternalId(), userEntity.getLoginCounter());
                        } else {
                            logger.info("Failed OTP attempt - user_id={}, login_counter={}. Invalid second factor attempt.", userEntity.getExternalId(), userEntity.getLoginCounter());
                        }
                        return Optional.<User>empty();
                    }
                })
                .orElseGet(() -> {
                    //this cannot happen unless a bug in selfservice
                    logger.error("Authenticate 2FA token attempted for non-existent User [{}]", externalId);
                    return Optional.empty();
                });
    }

    @Transactional
    public Optional<User> provisionNewOtpKey(String externalId) {
        return userDao.findByExternalId(externalId)
                .map(userEntity -> {
                    if (userEntity.isDisabled()) {
                        logger.warn("Attempt to provision a new OTP key for disabled user {}", userEntity.getExternalId());
                        return Optional.<User>empty();
                    }
                    logger.info("Provisioning new OTP key for user {}", userEntity.getExternalId());
                    ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
                    userEntity.setProvisionalOtpKey(secondFactorAuthenticator.generateNewBase32EncodedSecret());
                    userEntity.setProvisionalOtpKeyCreatedAt(now);
                    userEntity.setUpdatedAt(now);
                    userDao.merge(userEntity);
                    return Optional.of(linksBuilder.decorate(userEntity.toUser()));
                }).orElseGet(() -> {
                    logger.error("Attempt to provision a new OTP key for a non-existent user {}", externalId);
                    return Optional.empty();
                });
    }

    @Transactional
    public Optional<User> activateNewOtpKey(String externalId, SecondFactorMethod secondFactor, int code) {
        return userDao.findByExternalId(externalId)
                .map(userEntity -> {
                    if (userEntity.isDisabled()) {
                        logger.error("Attempt to activate a new OTP key for disabled user {}", userEntity.getExternalId());
                        return Optional.<User>empty();
                    }

                    if (userEntity.getProvisionalOtpKey() == null) {
                        logger.error("Attempt to activate a new OTP key for user {} without a provisional one", userEntity.getExternalId());
                        return Optional.<User>empty();
                    }

                    ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
                    ZonedDateTime provisionalOtpKeyCreatedAt = userEntity.getProvisionalOtpKeyCreatedAt();
                    if (provisionalOtpKeyCreatedAt == null || provisionalOtpKeyCreatedAt.plusMinutes(90).isBefore(now)) {
                        logger.warn("Attempt to activate a new OTP key for user {} but provisional one was created too long ago at {}",
                                userEntity.getExternalId(), provisionalOtpKeyCreatedAt);
                        return Optional.<User>empty();
                    }

                    if (!secondFactorAuthenticator.authorize(userEntity.getProvisionalOtpKey(), code)) {
                        logger.info("Attempt to activate a new OTP key for user {} with incorrect code", userEntity.getExternalId());
                        return Optional.<User>empty();
                    }

                    logger.info("Activating new OTP key and method {} for user {}", secondFactor.toString(), userEntity.getExternalId());
                    userEntity.setOtpKey(userEntity.getProvisionalOtpKey());
                    userEntity.setSecondFactor(secondFactor);
                    userEntity.setProvisionalOtpKey(null);
                    userEntity.setProvisionalOtpKeyCreatedAt(null);
                    userEntity.setUpdatedAt(now);
                    userDao.merge(userEntity);
                    return Optional.of(linksBuilder.decorate(userEntity.toUser()));
                }).orElseGet(() -> {
                    logger.error("Attempt to activate a new OTP key for a non-existent user {}", externalId);
                    return Optional.empty();
                });
    }

    @Transactional
    public Optional<User> resetSecondFactor(String externalId) {
        return userDao.findByExternalId(externalId).map(userEntity -> {
            if (userEntity.getSecondFactor().equals(SMS)) {
                logger.info("Second factor method is already SMS, doing nothing");
                return linksBuilder.decorate(userEntity.toUser());
            }
            if (userEntity.getTelephoneNumber().isEmpty()) {
                throw AdminUsersExceptions.cannotResetSecondFactorToSmsError(externalId);
            }
            
            logger.info("Resetting OTP method to SMS for user {}", userEntity.getExternalId());
            userEntity.setOtpKey(secondFactorAuthenticator.generateNewBase32EncodedSecret());
            userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
            userEntity.setSecondFactor(SMS);
            userDao.merge(userEntity);
            
            return linksBuilder.decorate(userEntity.toUser());
        });
    }

    @Transactional
    public Optional<User> patchUser(String externalId, PatchRequest patchRequest) {

        Optional<UserEntity> userOptional = userDao.findByExternalId(externalId);

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        UserEntity user = userOptional.get();
        
        switch (patchRequest.getPath()) {
            case PATH_SESSION_VERSION:
                incrementSessionVersion(user, parseInt(patchRequest.getValue()));
                break;
            case PATH_DISABLED:
                changeUserDisabled(user, parseBoolean(patchRequest.getValue()));
                break;
            case PATH_TELEPHONE_NUMBER:
                changeUserTelephoneNumber(user, patchRequest.getValue());
                break;
            case PATH_EMAIL:
                changeUserEmail(user, patchRequest.getValue());
                break;
            case PATH_FEATURES:
                changeUserFeatures(user, patchRequest.getValue());
                break;
            default:
                String error = format("Invalid patch request with path [%s]", patchRequest.getPath());
                logger.error(error);
                throw new RuntimeException(error);
                
        }
        
        return Optional.of(linksBuilder.decorate(user.toUser()));
    }
    
    public List<UserEntity> getAdminUsersForService(Integer serviceId) {
        List<UserEntity> serviceUsers = userDao.findByServiceId(serviceId);
        return serviceUsers.stream().filter(userEntity -> {
            var hasAdminRole = getServicesRoles(userEntity).stream().filter(RoleEntity::isAdmin).count();
            return hasAdminRole > 0;
        }).collect(Collectors.toList());
    }

    public List<RoleEntity> getServicesRoles(UserEntity userEntity) {
        return userEntity.getServicesRoles().isEmpty() ? emptyList() : singletonList(userEntity.getServicesRoles().get(0).getRole());
    }

    private void changeUserFeatures(UserEntity userEntity, String features) {
        userEntity.setFeatures(features);
        userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
        userDao.merge(userEntity);
    }

    private void changeUserTelephoneNumber(UserEntity userEntity, String telephoneNumber) {
        userEntity.setTelephoneNumber(TelephoneNumberUtility.formatToE164(telephoneNumber));
        userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
        userDao.merge(userEntity);
    }
    
    private void changeUserEmail(UserEntity userEntity, String email) {
        userEntity.setEmail(email);
        userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
        userDao.merge(userEntity);
    }

    private void changeUserDisabled(UserEntity userEntity, Boolean value) {
        userEntity.setLoginCounter(0);
        userEntity.setDisabled(value);
        userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
        userDao.merge(userEntity);
    }

    private void incrementSessionVersion(UserEntity userEntity, Integer value) {
        userEntity.setSessionVersion(userEntity.getSessionVersion() + value);
        userEntity.setUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
        userDao.merge(userEntity);
    }

}
