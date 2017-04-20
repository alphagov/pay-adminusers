package uk.gov.pay.adminusers.persistence.dao;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.ServiceDbFixture;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;

import javax.xml.ws.Service;
import java.util.List;
import java.util.Map;

import static java.sql.Timestamp.from;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;

public class InviteDaoTest extends DaoTestBase {

    private InviteDao inviteDao;
    private RoleDao roleDao;
    private ServiceDao serviceDao;

    @Before
    public void before() throws Exception {
        inviteDao = env.getInstance(InviteDao.class);
        roleDao = env.getInstance(RoleDao.class);
        serviceDao = env.getInstance(ServiceDao.class);
    }

    @Test
    public void create_shouldCreateAnInvite() {

        Role role = roleDbFixture(databaseHelper).insertRole();
        int serviceId = ServiceDbFixture.serviceDbFixture(databaseHelper).insertService();

        RoleEntity roleEntity = roleDao.findByRoleName(role.getName()).get();
        ServiceEntity serviceEntity = serviceDao.findById(serviceId).get();

        String email = "user@example.com";
        String code = randomAlphanumeric(10);

        InviteEntity invite = new InviteEntity(email, code, serviceEntity, roleEntity);

        inviteDao.persist(invite);

        List<Map<String, Object>> savedInvite = databaseHelper.findInviteById(invite.getId());

        assertThat(savedInvite.size(), is(1));
        assertThat(savedInvite.get(0).get("email"), is(email));
        assertThat(savedInvite.get(0).get("role_id"), is(roleEntity.getId()));
        assertThat(savedInvite.get(0).get("service_id"), is(serviceId));
        assertThat(savedInvite.get(0).get("code"), is(code));
        assertThat(savedInvite.get(0).get("date"), is(from(invite.getDate().toInstant())));
    }
}
