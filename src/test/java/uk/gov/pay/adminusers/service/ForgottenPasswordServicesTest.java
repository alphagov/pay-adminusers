package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.persistence.dao.ForgottenPasswordDao;
import uk.gov.pay.adminusers.persistence.dao.UserDao;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.time.ZonedDateTime;
import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ForgottenPasswordServicesTest {

    private ForgottenPasswordServices forgottenPasswordServices;
    private ForgottenPasswordDao forgottenPasswordDao;
    private UserDao userDao;

    @Before
    public void before() throws Exception {
        userDao = mock(UserDao.class);
        forgottenPasswordDao = mock(ForgottenPasswordDao.class);
        forgottenPasswordServices = new ForgottenPasswordServices(userDao, forgottenPasswordDao, new LinksBuilder("http://localhost"));
    }

    @Test
    public void shouldReturnANewForgottenPassword_whenCreating_ifUserFound() throws Exception {
        String existingUsername = "existing-user";
        UserEntity mockUser = mock(UserEntity.class);
        when(mockUser.getUsername()).thenReturn(existingUsername);

        when(userDao.findByUsername(existingUsername)).thenReturn(Optional.of(mockUser));
        Optional<ForgottenPassword> forgottenPasswordOptional = forgottenPasswordServices.create(existingUsername);

        verify(forgottenPasswordDao, times(1)).persist(any(ForgottenPasswordEntity.class));
        assertTrue(forgottenPasswordOptional.isPresent());
        assertThat(forgottenPasswordOptional.get().getUsername(), is(existingUsername));
        assertThat(forgottenPasswordOptional.get().getCode(), is(notNullValue()));
        assertThat(forgottenPasswordOptional.get().getLinks().size(), is(1));
    }

    @Test
    public void shouldReturnEmpty_whenCreating_ifUserNotFound() throws Exception {
        String nonExistentUser = "non-existent-user";
        when(userDao.findByUsername(nonExistentUser)).thenReturn(Optional.empty());

        Optional<ForgottenPassword> forgottenPasswordOptional = forgottenPasswordServices.create(nonExistentUser);
        assertFalse(forgottenPasswordOptional.isPresent());
    }

    @Test
    public void shouldFindForgottenPassword_whenFindByCode_ifFound() throws Exception {
        String existingCode = "existing-code";
        ForgottenPasswordEntity forgottenPasswordEntity = mockForgottenPassword(existingCode);
        when(forgottenPasswordDao.findNonExpiredByCode(existingCode)).thenReturn(Optional.of(forgottenPasswordEntity));

        Optional<ForgottenPassword> forgottenPasswordOptional = forgottenPasswordServices.findNonExpired(existingCode);
        assertTrue(forgottenPasswordOptional.isPresent());
        assertThat(forgottenPasswordOptional.get().getCode(), is(existingCode));
        assertThat(forgottenPasswordOptional.get().getLinks(), hasSize(1));
    }

    @Test
    public void shouldReturnEmpty_whenFindByCode_ifNotFound() throws Exception {
        String nonExistentCode = "non-existent-code";
        when(forgottenPasswordDao.findNonExpiredByCode(nonExistentCode)).thenReturn(Optional.empty());

        Optional<ForgottenPassword> forgottenPasswordOptional = forgottenPasswordServices.findNonExpired(nonExistentCode);
        assertFalse(forgottenPasswordOptional.isPresent());
    }

    private ForgottenPasswordEntity mockForgottenPassword(String code) {
        UserEntity mockUser = mock(UserEntity.class);
        when(mockUser.getUsername()).thenReturn("random-username");
        return new ForgottenPasswordEntity(code, ZonedDateTime.now(), mockUser);
    }
}
