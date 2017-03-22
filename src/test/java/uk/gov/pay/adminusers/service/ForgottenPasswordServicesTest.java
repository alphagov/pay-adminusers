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
    public void shouldReturnANewForgottenPassword_whenCreatingUsingUserExternalId_ifUserFound() throws Exception {
        String existingUserExternalId = "7d19aff33f8948deb97ed16b2912dcd3";
        UserEntity mockUser = mock(UserEntity.class);
        when(mockUser.getExternalId()).thenReturn(existingUserExternalId);

        when(userDao.findByUsername(existingUserExternalId)).thenReturn(Optional.empty());
        when(userDao.findByExternalId(existingUserExternalId)).thenReturn(Optional.of(mockUser));
        Optional<ForgottenPassword> forgottenPasswordOptional = forgottenPasswordServices.create(existingUserExternalId);

        verify(forgottenPasswordDao, times(1)).persist(any(ForgottenPasswordEntity.class));
        assertTrue(forgottenPasswordOptional.isPresent());
        assertThat(forgottenPasswordOptional.get().getUserExternalId(), is(existingUserExternalId));
        assertThat(forgottenPasswordOptional.get().getCode(), is(notNullValue()));
        assertThat(forgottenPasswordOptional.get().getLinks().size(), is(1));
    }

    @Test
    public void shouldReturnANewForgottenPassword_whenCreatingUsingUsername_ifUserFound() throws Exception {
        String existingUser = "existing-user";
        UserEntity mockUser = mock(UserEntity.class);
        when(mockUser.getUsername()).thenReturn(existingUser);

        when(userDao.findByUsername(existingUser)).thenReturn(Optional.of(mockUser));
        Optional<ForgottenPassword> forgottenPasswordOptional = forgottenPasswordServices.create(existingUser);

        verify(forgottenPasswordDao, times(1)).persist(any(ForgottenPasswordEntity.class));
        assertTrue(forgottenPasswordOptional.isPresent());
        assertThat(forgottenPasswordOptional.get().getUsername(), is(existingUser));
        assertThat(forgottenPasswordOptional.get().getCode(), is(notNullValue()));
        assertThat(forgottenPasswordOptional.get().getLinks().size(), is(1));
    }

    @Test
    public void shouldReturnEmpty_whenCreating_ifUserNotFound() throws Exception {
        String nonExistingUser = "non-existent-user";
        when(userDao.findByUsername(nonExistingUser)).thenReturn(Optional.empty());
        when(userDao.findByExternalId(nonExistingUser)).thenReturn(Optional.empty());

        Optional<ForgottenPassword> forgottenPasswordOptional = forgottenPasswordServices.create(nonExistingUser);
        assertFalse(forgottenPasswordOptional.isPresent());
    }

    @Test
    public void shouldFindForgottenPassword_whenFindByCode_ifFound() throws Exception {
        String code = "an-existent-code";
        ForgottenPasswordEntity forgottenPasswordEntity = mockForgottenPassword(code);
        when(forgottenPasswordDao.findNonExpiredByCode(code)).thenReturn(Optional.of(forgottenPasswordEntity));

        Optional<ForgottenPassword> forgottenPasswordOptional = forgottenPasswordServices.findNonExpired(code);
        assertTrue(forgottenPasswordOptional.isPresent());
        assertThat(forgottenPasswordOptional.get().getCode(), is(code));
        assertThat(forgottenPasswordOptional.get().getLinks(), hasSize(1));
    }

    @Test
    public void shouldReturnEmpty_whenFindByCode_ifNotFound() throws Exception {
        String code = "non-existent-code";
        when(forgottenPasswordDao.findNonExpiredByCode(code)).thenReturn(Optional.empty());

        Optional<ForgottenPassword> forgottenPasswordOptional = forgottenPasswordServices.findNonExpired(code);
        assertFalse(forgottenPasswordOptional.isPresent());
    }

    private ForgottenPasswordEntity mockForgottenPassword(String code) {
        UserEntity mockUser = mock(UserEntity.class);
        when(mockUser.getUsername()).thenReturn("random-username");
        return new ForgottenPasswordEntity(code, ZonedDateTime.now(), mockUser);
    }
}
