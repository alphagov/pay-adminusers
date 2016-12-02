package uk.gov.pay.adminusers.service;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PasswordHasherTest {

    @Test
    public void shouldHashAPlainTextPassword() throws Exception {
        PasswordHasher passwordHasher = new PasswordHasher();

        String hashedPassword = passwordHasher.hash("plain text password");
        assertThat(hashedPassword, is(not("plain text password")));
    }

    @Test
    public void shouldSuccessfullyCompareExistingHashWithAPlaintextPassword() throws Exception {
        PasswordHasher passwordHasher = new PasswordHasher();
        String hashedPassword = passwordHasher.hash("plain text password");

        assertTrue(passwordHasher.isEqual("plain text password", hashedPassword));
    }

    @Test
    public void shouldFalsify_IfPasswordAndHashDontMatch() throws Exception {
        PasswordHasher passwordHasher = new PasswordHasher();
        String hashedPassword = passwordHasher.hash("plain text password");

        assertFalse(passwordHasher.isEqual("different password", hashedPassword));
    }
}
