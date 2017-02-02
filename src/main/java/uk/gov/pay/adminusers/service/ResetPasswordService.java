package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.Optional;

public class ResetPasswordService {

    private final UserDao userDao;
    private final ForgottenPasswordDao forgottenPasswordDao;
    private final PasswordHasher passwordHasher;

    @Inject
    public ResetPasswordService(UserDao userDao, ForgottenPasswordDao forgottenPasswordDao, PasswordHasher passwordHasher) {
        this.userDao = userDao;
        this.forgottenPasswordDao = forgottenPasswordDao;
        this.passwordHasher = passwordHasher;
    }

    public Optional<Integer> updatePassword(String code, String password) {
        return forgottenPasswordDao.findNonExpiredByCode(code).map(forgottenPassword -> {
            UserEntity userEntity = forgottenPassword.getUser();
            userEntity.setLoginCount(0);
            userEntity.setPassword(passwordHasher.hash(password));
            userDao.merge(userEntity);
            forgottenPasswordDao.remove(forgottenPassword);
            return Optional.of(userEntity.getId());
        }).orElseGet(Optional::empty);
    }
}
