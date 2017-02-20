package uk.gov.pay.adminusers.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SecondFactorAuthenticatorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private SecondFactorAuthenticator secondFactorAuthenticator;
    @Before
    public void before() throws Exception {
        secondFactorAuthenticator = new SecondFactorAuthenticator(60);
    }

    @Test
    public void shouldGenerateAndValidate2FAPasscode() throws Exception {
        String secret = "mysecret";
        Integer password = secondFactorAuthenticator.newPassCode(secret);

        assertTrue(secondFactorAuthenticator.authorize(secret, password));
        assertFalse(secondFactorAuthenticator.authorize(secret + 1, password));
    }


    @Test
    public void shouldError_IfPasscodeIsNull_WhenCreate() throws Exception {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("supplied a null/empty otpKey for second factor");

        secondFactorAuthenticator.newPassCode(null);
    }
}
