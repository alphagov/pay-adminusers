package uk.gov.pay.adminusers.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import jersey.repackaged.com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.resources.ResetPasswordValidator;
import uk.gov.pay.adminusers.utils.Errors;

import java.util.Optional;

public class ResetPasswordService {

    private static final Logger logger = PayLoggerFactory.getLogger(ForgottenPasswordServices.class);
    private final UserDao userDao;
    private final ForgottenPasswordDao forgottenPasswordDao;
    private final PasswordHasher passwordHasher;
    private ResetPasswordValidator validator;

    @Inject
    public ResetPasswordService(UserDao userDao, ForgottenPasswordDao forgottenPasswordDao, PasswordHasher passwordHasher) {
        this.userDao = userDao;
        this.forgottenPasswordDao = forgottenPasswordDao;
        this.passwordHasher = passwordHasher;
        this.validator = new ResetPasswordValidator();
    }

    public Optional<Errors> updatePassword(JsonNode payload) {

        Optional<Errors> optionalErrors = validator.validateResetRequest(payload);

        if (optionalErrors.isPresent()) {
            logger.warn("Password validation failed, JsonNode invalid", optionalErrors.get().getErrors().toString());
            return optionalErrors;
        }

        Optional<ForgottenPasswordEntity> optionalForgottenPasswordEntity =
                forgottenPasswordDao.findNonExpiredByCode(payload.get("forgotten_password_code").asText());

        if (!optionalForgottenPasswordEntity.isPresent()) {
            logger.warn("Password validation failed, no valid Entity found by " + payload.get("forgotten_password_code"));
            return Optional.of(Errors.from(ImmutableList.of("Field [forgotten_password_code] has expired")));
        }

        ForgottenPasswordEntity forgottenPasswordEntity = optionalForgottenPasswordEntity.get();

        UserEntity userEntity = forgottenPasswordEntity.getUser();
        userEntity.setLoginCount(0);
        userEntity.setPassword(passwordHasher.hash(payload.get("new_password").asText()));

        userDao.merge(userEntity);
        forgottenPasswordDao.remove(forgottenPasswordEntity);

        logger.info("Successfully changed password");

        return Optional.empty();
    }
}
