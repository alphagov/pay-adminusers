package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import jersey.repackaged.com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.Optional;

import static uk.gov.pay.adminusers.service.AdminUsersExceptions.*;
import static uk.gov.pay.adminusers.utils.Errors.from;

public class ResetPasswordService {

    private static final Logger logger = PayLoggerFactory.getLogger(ResetPasswordService.class);
    private final UserDao userDao;
    private final ForgottenPasswordDao forgottenPasswordDao;
    private final PasswordHasher passwordHasher;

    @Inject
    public ResetPasswordService(UserDao userDao, ForgottenPasswordDao forgottenPasswordDao, PasswordHasher passwordHasher) {
        this.userDao = userDao;
        this.forgottenPasswordDao = forgottenPasswordDao;
        this.passwordHasher = passwordHasher;
    }

    public void updatePassword(String code, String password) {

        Optional<ForgottenPasswordEntity> forgottenPassword = forgottenPasswordDao.findNonExpiredByCode(code);

        if (forgottenPassword.isPresent()) {
            ForgottenPasswordEntity entity = forgottenPassword.get();
            UserEntity userEntity = entity.getUser();
            logger.info("User {} is about to change password. user_id={}", userEntity.getId());
            userEntity.setLoginCount(0);
            userEntity.setPassword(passwordHasher.hash(password));
            userDao.merge(userEntity);
            forgottenPasswordDao.remove(entity);
        } else {
            throw clientErrorException(from(ImmutableList.of("Field [forgotten_password_code] non-existent/expired")));
        }
    }
}
