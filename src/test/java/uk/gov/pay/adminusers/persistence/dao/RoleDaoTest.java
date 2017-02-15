package uk.gov.pay.adminusers.persistence.dao;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;

import java.util.Optional;

import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RoleDaoTest extends DaoTestBase {

    private RoleDao roleDao;

    @Before
    public void before() throws Exception {
        roleDao = env.getInstance(RoleDao.class);
    }

    @Test
    public void shouldFindARoleByRoleName() throws Exception {
        Permission perm1 = aPermission();
        Permission perm2 = aPermission();
        Permission perm3 = aPermission();
        Permission perm4 = aPermission();
        Role role1 = aRole();
        Role role2 = aRole();
        role1.setPermissions(asList(perm1, perm2));
        role2.setPermissions(asList(perm3, perm4));

        databaseTestHelper.add(perm1).add(perm2).add(perm3).add(perm4);
        databaseTestHelper.add(role1).add(role2);

        Optional<RoleEntity> optionalRole1 = roleDao.findByRoleName(role1.getName());
        assertTrue(optionalRole1.isPresent());

        RoleEntity roleEntity = optionalRole1.get();
        assertThat(roleEntity.toRole(), is(role1));

        Optional<RoleEntity> optionalRole2 = roleDao.findByRoleName(role2.getName());
        assertThat(optionalRole2.get().toRole(),is(role2));

    }

}
