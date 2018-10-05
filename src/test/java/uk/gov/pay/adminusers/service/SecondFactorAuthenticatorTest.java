package uk.gov.pay.adminusers.service;

import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SecondFactorAuthenticatorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String SECRET = "mysecret";
    private static final String BASE32_ENCODED_SECRET = "KPWXGUTNWOE7PMVK";

    private static final Duration TIME_STEP = Duration.of(30, SECONDS);

    private static final int PAST_OR_FUTURE_WINDOWS_TO_CHECK = 4;
    private static final int PAST_PRESENT_AND_FUTURE_WINDOWS_TO_CHECK = PAST_OR_FUTURE_WINDOWS_TO_CHECK * 2 + 1;

    private static final GoogleAuthenticatorConfig AUTH_CONFIG = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
            .setWindowSize(PAST_PRESENT_AND_FUTURE_WINDOWS_TO_CHECK)
            .setTimeStepSizeInMillis(TIME_STEP.toMillis())
            .build();

    @Mock
    private Clock clock;

    private Instant initialTime;

    private SecondFactorAuthenticator secondFactorAuthenticator;

    @Before
    public void before() {
        initialTime = Instant.now();
        when(clock.millis()).thenReturn(initialTime.toEpochMilli());
        secondFactorAuthenticator = new SecondFactorAuthenticator(AUTH_CONFIG, clock);
    }

    @Test
    public void shouldGenerateAndValidate2FAPasscode() {
        int passCode = secondFactorAuthenticator.newPassCode(SECRET);

        assertTrue(secondFactorAuthenticator.authorize(SECRET, passCode));
        assertFalse(secondFactorAuthenticator.authorize("incorrectSecret", passCode));
    }

    @Test
    public void shouldGenerateAndValidate2FAPasscodeFromBase32EncodedSecret() {
        int passCode = secondFactorAuthenticator.newPassCode(BASE32_ENCODED_SECRET);

        assertTrue(secondFactorAuthenticator.authorize(BASE32_ENCODED_SECRET, passCode));
    }

    @Test
    public void shouldSuccess_ifAskedToValidateImmediateLastSteps2FAPasscode() {
        int passCode = secondFactorAuthenticator.newPassCode(SECRET);

        when(clock.millis()).thenReturn(initialTime.plus(TIME_STEP).toEpochMilli());

        assertTrue(secondFactorAuthenticator.authorize(SECRET, passCode));
    }

    @Test
    public void shouldSuccess_ifAskedToValidateImmediateLastSteps2FAPasscode_fromBase32EncodedSecret() {
        int passCode = secondFactorAuthenticator.newPassCode(BASE32_ENCODED_SECRET);

        when(clock.millis()).thenReturn(initialTime.plus(TIME_STEP).toEpochMilli());

        assertTrue(secondFactorAuthenticator.authorize(BASE32_ENCODED_SECRET, passCode));
    }

    @Test
    public void shouldSuccess_ifAskedToValidateAValidPastSteps2FAPasscode() {
        int passCode = secondFactorAuthenticator.newPassCode(SECRET);

        when(clock.millis()).thenReturn(initialTime.plus(TIME_STEP.multipliedBy(PAST_OR_FUTURE_WINDOWS_TO_CHECK)).toEpochMilli());

        assertTrue(secondFactorAuthenticator.authorize(SECRET, passCode));
    }

    @Test
    public void shouldSuccess_ifAskedToValidateAValidPastSteps2FAPasscode_fromBase32EncodedSecret() {
        int passCode = secondFactorAuthenticator.newPassCode(BASE32_ENCODED_SECRET);

        when(clock.millis()).thenReturn(initialTime.plus(TIME_STEP.multipliedBy(PAST_OR_FUTURE_WINDOWS_TO_CHECK)).toEpochMilli());

        assertTrue(secondFactorAuthenticator.authorize(BASE32_ENCODED_SECRET, passCode));
    }

    @Test
    public void shouldError_ifAskedToValidate2FAPasscodeOlderThanLastValidStep() {
        int passCode = secondFactorAuthenticator.newPassCode(SECRET);

        when(clock.millis()).thenReturn(initialTime.plus(TIME_STEP.multipliedBy(PAST_OR_FUTURE_WINDOWS_TO_CHECK + 1)).toEpochMilli());

        assertFalse(secondFactorAuthenticator.authorize(SECRET, passCode));
    }

    @Test
    public void shouldError_ifAskedToValidate2FAPasscodeOlderThanLastValidStep_fromBase32EncodedSecret() {
        int passCode = secondFactorAuthenticator.newPassCode(BASE32_ENCODED_SECRET);

        when(clock.millis()).thenReturn(initialTime.plus(TIME_STEP.multipliedBy(PAST_OR_FUTURE_WINDOWS_TO_CHECK + 1)).toEpochMilli());

        assertFalse(secondFactorAuthenticator.authorize(BASE32_ENCODED_SECRET, passCode));
    }

    @Test
    public void shouldError_IfPasscodeIsNull_WhenCreate() {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("supplied a null/empty otpKey for second factor");

        secondFactorAuthenticator.newPassCode(null);
    }

    @Test
    public void shouldGenerateNewBase32EncodedSecret() {
        String sixteenCharacterBase32Regex = "[A-Z2-7]{16}";
        assertThat(secondFactorAuthenticator.generateNewBase32EncodedSecret(), matchesPattern(sixteenCharacterBase32Regex));
    }

}
