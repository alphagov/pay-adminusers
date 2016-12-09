package uk.gov.pay.adminusers.service;

import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AdminUsersExceptionsTest {

    @Test
    public void shouldCreateARoleUnavailabeException() throws Exception {
        WebApplicationException undefinedRoleException = AdminUsersExceptions.undefinedRoleException("non-existent-role");
        assertThat(undefinedRoleException.getResponse().getStatus(), is(400));
        Map<String, List<String>> entity = (Map<String, List<String>>) undefinedRoleException.getResponse().getEntity();
        assertThat(entity.get("errors").get(0), is("role [non-existent-role] not recognised"));
    }

    @Test
    public void shouldCreateAConflictingUsernameException() throws Exception {
        WebApplicationException undefinedRoleException = AdminUsersExceptions.conflictingUsername("existing-user");
        assertThat(undefinedRoleException.getResponse().getStatus(), is(409));
        Map<String, List<String>> entity = (Map<String, List<String>>) undefinedRoleException.getResponse().getEntity();
        assertThat(entity.get("errors").get(0), is("username [existing-user] already exists"));
    }

    @Test
    public void shouldCreateAnInternalServerErrorException() throws Exception {
        WebApplicationException undefinedRoleException = AdminUsersExceptions.internalServerError("server error");
        assertThat(undefinedRoleException.getResponse().getStatus(), is(500));
        Map<String, List<String>> entity = (Map<String, List<String>>) undefinedRoleException.getResponse().getEntity();
        assertThat(entity.get("errors").get(0), is("server error"));
    }

    @Test
    public void shouldCreateAnUnAuthorizedLockedException() throws Exception {
        WebApplicationException undefinedRoleException = AdminUsersExceptions.userLockedException("bad-user");
        assertThat(undefinedRoleException.getResponse().getStatus(), is(401));
        Map<String, List<String>> entity = (Map<String, List<String>>) undefinedRoleException.getResponse().getEntity();
        assertThat(entity.get("errors").get(0), is("user [bad-user] locked due to too many login attempts"));
    }
}
