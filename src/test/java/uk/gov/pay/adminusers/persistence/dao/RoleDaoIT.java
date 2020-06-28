package uk.gov.pay.adminusers.persistence.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.Role;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

public class RoleDaoIT extends DaoTestBase {

    private RoleDao roleDao;

    @BeforeEach
    public void before() {
        roleDao = env.getInstance(RoleDao.class);
    }

    @Test
    public void shouldFindSuperAdmin() {
        findAndCheckRole("super-admin", "Super Admin");
    }

    @Test
    public void shouldFindViewAndRefund() {
        findAndCheckRole("view-and-refund", "View and Refund");
    }

    @Test
    public void shouldReturnEmptyOptionalForNonexistentRole() {
        assertTrue(roleDao.findByRoleName("does-not-exist").isEmpty());
    }

    private void findAndCheckRole(String name, String description) {
        Role role = roleDao.findByRoleName(name)
                .orElseThrow(() -> new RuntimeException(format("Role '%s' not found", name)))
                .toRole();

        assertThat(description, is(role.getDescription()));
    }
}
