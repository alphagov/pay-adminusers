package uk.gov.pay.adminusers.service;

import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecondFactorAuthenticatorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String SECRET = "mysecret";
    private static final Instant INITIAL_TIME = Instant.now();
    private static final Duration TIME_STEP = Duration.of(30, SECONDS);
    private static final int PAST_OR_FUTURE_WINDOWS_TO_CHECK = 4;
    private static final int PAST_PRESENT_AND_FUTURE_WINDOWS_TO_CHECK = PAST_OR_FUTURE_WINDOWS_TO_CHECK * 2 + 1;
    private static final GoogleAuthenticatorConfig authConfig = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
            .setWindowSize(PAST_PRESENT_AND_FUTURE_WINDOWS_TO_CHECK)
            .setTimeStepSizeInMillis(TIME_STEP.toMillis())
            .build();



    private Integer passcode;
    private SecondFactorAuthenticator secondFactorAuthenticator;
    private Clock clock;


    @Before
    public void before() throws Exception {
        clock = mock(Clock.class);
        when(clock.millis()).thenReturn(INITIAL_TIME.toEpochMilli());
        secondFactorAuthenticator = new SecondFactorAuthenticator(authConfig, clock);
        passcode = secondFactorAuthenticator.newPassCode(SECRET);
        
    }

    @Test
    public void shouldGenerateAndValidate2FAPasscode() throws Exception {
        assertTrue(secondFactorAuthenticator.authorize(SECRET, passcode));
        assertFalse(secondFactorAuthenticator.authorize("incorrectSecret", passcode));
    }

    @Test
    public void shouldSuccess_ifAskedToValidateImmediateLastSteps2FAPasscode() throws Exception {
        when(clock.millis()).thenReturn(INITIAL_TIME.plus(TIME_STEP).toEpochMilli());

        assertTrue(secondFactorAuthenticator.authorize(SECRET, passcode));
    }

    @Test
    public void shouldSuccess_ifAskedToValidateAValidPastSteps2FAPasscode() throws Exception {
        when(clock.millis()).thenReturn(INITIAL_TIME.plus(TIME_STEP.multipliedBy(PAST_OR_FUTURE_WINDOWS_TO_CHECK)).toEpochMilli());

        assertTrue(secondFactorAuthenticator.authorize(SECRET, passcode));
    }

    @Test
    public void shouldError_ifAskedToValidate2FAPasscodeOlderThanLastValidStep() throws Exception {
        when(clock.millis()).thenReturn(INITIAL_TIME.plus(TIME_STEP.multipliedBy(PAST_OR_FUTURE_WINDOWS_TO_CHECK + 1)).toEpochMilli());

        assertFalse(secondFactorAuthenticator.authorize(SECRET, passcode));
    }

    @Test
    public void shouldError_IfPasscodeIsNull_WhenCreate() throws Exception {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("supplied a null/empty otpKey for second factor");

        secondFactorAuthenticator.newPassCode(null);
    }
}
