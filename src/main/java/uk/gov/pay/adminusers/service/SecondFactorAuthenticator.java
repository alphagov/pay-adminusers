package uk.gov.pay.adminusers.service;

import com.google.inject.Inject;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.regex.Pattern;

import static com.google.common.io.BaseEncoding.base32;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class SecondFactorAuthenticator {

    private static final Logger logger = PayLoggerFactory.getLogger(SecondFactorAuthenticator.class);

    private static final Pattern BASE32_ALPHABET = Pattern.compile("[A-Z2-7]+");

    private final GoogleAuthenticator authenticator;
    private final Clock clock;

    @Inject
    public SecondFactorAuthenticator(GoogleAuthenticatorConfig authenticatorConfig, Clock clock) {
        this.clock = clock;
        this.authenticator = new GoogleAuthenticator(authenticatorConfig);
    }

    public int newPassCode(String secret) {
        checkNull(secret);
        String base32EncodedSecret = BASE32_ALPHABET.matcher(secret).matches() ? secret : base32EncodedUtf8BytesOfSecret(secret);
        return authenticator.getTotpPassword(base32EncodedSecret, clock.millis());
    }

    public boolean authorize(String secret, int passcode) {
        checkNull(secret);
        String base32EncodedSecret = BASE32_ALPHABET.matcher(secret).matches() ? secret : base32EncodedUtf8BytesOfSecret(secret);
        return authenticator.authorize(base32EncodedSecret, passcode, clock.millis());
    }

    public String generateNewBase32EncodedSecret() {
        return authenticator.createCredentials().getKey();
    }

    private String base32EncodedUtf8BytesOfSecret(String secret) {
        // This seems to be to match the recommendations of notp, a
        // Node.js package we used to use to do OTP in self-service
        // https://github.com/guyht/notp/blob/master/Readme.md#google-authenticator
        return base32().encode(secret.getBytes(StandardCharsets.UTF_8));
    }

    private void checkNull(String secret) {
        if (isBlank(secret)) {
            String error = "supplied a null/empty otpKey for second factor";
            logger.error(error);
            throw new RuntimeException(error);
        }
    }

}
