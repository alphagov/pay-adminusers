package uk.gov.pay.adminusers.service;

import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecondFactorAuthenticatorTest {
    
    private static final String BASE32_ENCODED_SECRET = "KPWXGUTNWOE7PMVK";

    private static final Duration TIME_STEP = Duration.of(30, SECONDS);

    private static final int PAST_OR_FUTURE_WINDOWS_TO_CHECK = 4;
    private static final int PAST_PRESENT_AND_FUTURE_WINDOWS_TO_CHECK = PAST_OR_FUTURE_WINDOWS_TO_CHECK * 2 + 1;

    private static final GoogleAuthenticatorConfig AUTH_CONFIG = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
            .setWindowSize(PAST_PRESENT_AND_FUTURE_WINDOWS_TO_CHECK)
            .setTimeStepSizeInMillis(TIME_STEP.toMillis())
            .build();

    @Mock
    private InstantSource instantSource;

    private Instant initialTime;

    private SecondFactorAuthenticator secondFactorAuthenticator;

    @BeforeEach
    void before() {
        initialTime = Instant.now();
        lenient().when(instantSource.millis()).thenReturn(initialTime.toEpochMilli());
        secondFactorAuthenticator = new SecondFactorAuthenticator(AUTH_CONFIG, instantSource);
    }

    @Test
    void shouldGenerateAndValidate2FAPasscode() {
        int passCode = secondFactorAuthenticator.newPassCode(BASE32_ENCODED_SECRET);

        assertTrue(secondFactorAuthenticator.authorize(BASE32_ENCODED_SECRET, passCode));
        
        String incorrectSecret = "7FLZNEDRXUVBN7OJQVCGUO344R4LO22Y";
        assertFalse(secondFactorAuthenticator.authorize(incorrectSecret, passCode));
    }

    @Test
    void shouldSuccess_ifAskedToValidateImmediateLastSteps2FAPasscode() {
        int passCode = secondFactorAuthenticator.newPassCode(BASE32_ENCODED_SECRET);

        when(instantSource.millis()).thenReturn(initialTime.plus(TIME_STEP).toEpochMilli());

        assertTrue(secondFactorAuthenticator.authorize(BASE32_ENCODED_SECRET, passCode));
    }

    @Test
    void shouldSuccess_ifAskedToValidateAValidPastSteps2FAPasscode() {
        int passCode = secondFactorAuthenticator.newPassCode(BASE32_ENCODED_SECRET);

        when(instantSource.millis()).thenReturn(initialTime.plus(TIME_STEP.multipliedBy(PAST_OR_FUTURE_WINDOWS_TO_CHECK)).toEpochMilli());

        assertTrue(secondFactorAuthenticator.authorize(BASE32_ENCODED_SECRET, passCode));
    }

    @Test
    void shouldError_ifAskedToValidate2FAPasscodeOlderThanLastValidStep() {
        int passCode = secondFactorAuthenticator.newPassCode(BASE32_ENCODED_SECRET);

        when(instantSource.millis()).thenReturn(initialTime.plus(TIME_STEP.multipliedBy(PAST_OR_FUTURE_WINDOWS_TO_CHECK + 1)).toEpochMilli());

        assertFalse(secondFactorAuthenticator.authorize(BASE32_ENCODED_SECRET, passCode));
    }

    @Test
    void shouldError_IfPasscodeIsNull_WhenCreate() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> secondFactorAuthenticator.newPassCode(null));
        assertThat(exception.getMessage(), is("supplied a null/empty otpKey for second factor"));
    }

    @Test
    void shouldGenerateNewBase32EncodedSecret() {
        String thirtyTwoCharacterBase32Regex = "[A-Z2-7]{32}";
        assertThat(secondFactorAuthenticator.generateNewBase32EncodedSecret(), matchesPattern(thirtyTwoCharacterBase32Regex));
    }

}
