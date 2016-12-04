package uk.gov.pay.adminusers.service;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PasswordHasherTest {

    public static final String HASH_SALT = "$2a$10$IhaXo6LIBhKIWOiGpbtPOu";

    @Test
    public void shouldHashAPlainTextPassword() throws Exception {
        PasswordHasher passwordHasher = new PasswordHasher(HASH_SALT);

        String hashedPassword = passwordHasher.hash("plain text password");
        assertThat(hashedPassword, is(not("plain text password")));
    }

    @Test
    public void shouldRegenerateSameHash_ifSamePasswordHashedTwice() throws Exception {
        PasswordHasher passwordHasher = new PasswordHasher(HASH_SALT);
        String hashedPassword = passwordHasher.hash("plain text password");
        String hashedPassword2 = passwordHasher.hash("plain text password");

        assertThat(hashedPassword,is(hashedPassword2));
    }

}
