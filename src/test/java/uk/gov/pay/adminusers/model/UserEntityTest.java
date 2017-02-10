package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;

public class UserEntityTest {

    @Test
    public void shouldConstructAUser_fromMinimalValidUserJson() throws Exception {
        String minimumUserJson = "{" +
                "\"username\": \"a-username\"," +
                "\"telephone_number\": \"2123524\"," +
                "\"gateway_account_id\": \"2\"," +
                "\"email\": \"email@example.com\"" +
                "}";

        JsonNode jsonNode = new ObjectMapper().readTree(minimumUserJson);
        User user = User.from(jsonNode);
        UserEntity userEntity = UserEntity.from(user);

        assertEquals(user.getUsername(), userEntity.getUsername());
        assertEquals(user.getPassword(), userEntity.getPassword());
        assertEquals(user.getOtpKey(), userEntity.getOtpKey());
        assertEquals(user.getTelephoneNumber(), userEntity.getTelephoneNumber());
        assertEquals(user.getEmail(), userEntity.getEmail());
        assertEquals(user.getRoles().size(), userEntity.getRoles().size());
        assertThat(userEntity.getCreatedAt(), is(notNullValue()));
        assertThat(userEntity.getUpdatedAt(), is(notNullValue()));
    }
}
