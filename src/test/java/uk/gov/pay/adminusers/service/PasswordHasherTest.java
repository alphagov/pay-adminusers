package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PasswordHasherTest {

    @Test
    public void shouldHashAPlainTextPassword() {
        PasswordHasher passwordHasher = new PasswordHasher();

        String hashedPassword = passwordHasher.hash("plain text password");
        assertThat(hashedPassword, is(not("plain text password")));
    }

    @Test
    public void shouldMatchToTrue_ifSamePassword() {
        PasswordHasher passwordHasher = new PasswordHasher();
        String hashedPassword = passwordHasher.hash("plain text password");

        assertTrue(passwordHasher.isEqual("plain text password",hashedPassword));
    }

    @Test
    public void shouldMatchToFalse_ifDifferentPassword() {
        PasswordHasher passwordHasher = new PasswordHasher();
        String hashedPassword = passwordHasher.hash("existing password");

        assertFalse(passwordHasher.isEqual("different password",hashedPassword));
    }
}
