package uk.gov.pay.adminusers.persistence.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.pay.adminusers.model.RoleName;

import static junit.framework.TestCase.assertTrue;

public class RoleDaoIT extends DaoTestBase {

    private RoleDao roleDao;

    @BeforeEach
    public void before() {
        roleDao = env.getInstance(RoleDao.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "super-admin", "admin", "view-and-refund", "view-only", 
            "view-and-initiate-moto", "view-refund-and-initiate-moto"
    })
    public void shouldFindARoleByRoleName(String name) {
        assertTrue(roleDao.findByRoleName(RoleName.fromName(name)).isPresent());
    }
}
