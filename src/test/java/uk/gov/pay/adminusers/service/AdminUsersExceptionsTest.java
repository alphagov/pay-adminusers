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
}
