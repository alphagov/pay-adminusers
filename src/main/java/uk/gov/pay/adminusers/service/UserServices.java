package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

public class UserServices {
    private final UserDao userDao;

    @Inject
    public UserServices(UserDao userDao) {
        this.userDao = userDao;
    }

    public User createUser(User validatedUserRequest) {
        UserEntity userEntity = UserEntity.from(validatedUserRequest);
        //encrypt password here
        userDao.persist(userEntity);
        return userEntity.toUser();
    }
}
