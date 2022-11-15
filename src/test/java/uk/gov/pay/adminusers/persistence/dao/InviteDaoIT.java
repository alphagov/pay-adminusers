package uk.gov.pay.adminusers.persistence.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.User;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;
import uk.gov.pay.adminusers.persistence.entity.ServiceEntity;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.sql.Timestamp.from;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.fixtures.InviteDbFixture.inviteDbFixture;
import static uk.gov.pay.adminusers.fixtures.RoleDbFixture.roleDbFixture;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

public class InviteDaoIT extends DaoTestBase {

    private InviteDao inviteDao;
    private RoleDao roleDao;
    private ServiceDao serviceDao;
    private UserDao userDao;

    @BeforeEach
    public void before() {
        inviteDao = env.getInstance(InviteDao.class);
        roleDao = env.getInstance(RoleDao.class);
        serviceDao = env.getInstance(ServiceDao.class);
        userDao = env.getInstance(UserDao.class);
    }

    @Test
    public void create_shouldCreateAnInvite() {

        Role role = roleDbFixture(databaseHelper).insertRole();
        int serviceId = serviceDbFixture(databaseHelper).insertService().getId();
        String username = randomUuid();
        String email = username + "@example.com";
        User sender = userDbFixture(databaseHelper).withUsername(username).withEmail(email).insertUser();

        RoleEntity roleEntity = roleDao.findByRoleName(role.getName()).get();
        ServiceEntity serviceEntity = serviceDao.findById(serviceId).get();
        UserEntity userSenderEntity = userDao.findById(sender.getId()).get();

        String code = randomAlphanumeric(10);
        String otpKey = randomAlphanumeric(10);

        InviteEntity invite = new InviteEntity("USER@example.com", code, otpKey, roleEntity);
        invite.setService(serviceEntity);
        invite.setSender(userSenderEntity);

        inviteDao.persist(invite);

        List<Map<String, Object>> savedInvite = databaseHelper.findInviteById(invite.getId());

        assertThat(savedInvite.size(), is(1));
        assertThat(savedInvite.get(0).get("sender_id"), is(userSenderEntity.getId()));
        assertThat(savedInvite.get(0).get("email"), is("user@example.com"));
        assertThat(savedInvite.get(0).get("role_id"), is(roleEntity.getId()));
        assertThat(savedInvite.get(0).get("service_id"), is(serviceId));
        assertThat(savedInvite.get(0).get("code"), is(code));
        assertThat(savedInvite.get(0).get("otp_key"), is(notNullValue()));
        assertThat(savedInvite.get(0).get("otp_key"), is(invite.getOtpKey()));
        assertThat(savedInvite.get(0).get("telephone_number"), is(nullValue()));
        assertThat(savedInvite.get(0).get("date"), is(from(invite.getDate().toInstant())));
        assertThat(savedInvite.get(0).get("disabled"), is(Boolean.FALSE));
        assertThat(savedInvite.get(0).get("login_counter"), is(0));
    }

    @Test
    public void findByCode_shouldFindAnExistingInvite() {

        String code = inviteDbFixture(databaseHelper).insertInviteToAddUserToService();

        Optional<InviteEntity> invite = inviteDao.findByCode(code);

        assertThat(invite.isPresent(), is(true));
    }

    @Test
    public void findByEmail_shouldFindAnExistingInvite() {

        String email = randomAlphanumeric(5) + "@example.com";

        inviteDbFixture(databaseHelper).withEmail(email).insertInviteToAddUserToService();

        List<InviteEntity> invites = inviteDao.findByEmail(email);

        assertThat(invites.isEmpty(), is(false));
    }

    @Test
    public void findAllByServiceId_shouldFindAllInvitesForAService() {
        String serviceId = "asfkhsjhfskdf";
        inviteDbFixture(databaseHelper).withServiceExternalId(serviceId).insertInviteToAddUserToService();

        List<InviteEntity> invites = inviteDao.findAllByServiceId(serviceId);
        
        assertThat(invites.size(), is(1));
    }
}
