package uk.gov.pay.adminusers.service;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SecondFactorAuthenticatorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldGenerateAndValidate2FAPasscode() throws Exception {
        String secret = "mysecret";
        Integer password = SecondFactorAuthenticator.newPassCode(secret);

        assertTrue(SecondFactorAuthenticator.authorize(secret, password));
        assertFalse(SecondFactorAuthenticator.authorize(secret + 1, password));
    }


    @Test
    public void shouldError_IfPasscodeIsNull_WhenCreate() throws Exception {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("supplied a null/empty otpKey for second factor");

        SecondFactorAuthenticator.newPassCode(null);
    }
}
