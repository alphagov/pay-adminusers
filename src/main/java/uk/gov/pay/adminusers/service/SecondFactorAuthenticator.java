package uk.gov.pay.adminusers.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.google.common.io.BaseEncoding.base32;
import static org.apache.commons.lang3.StringUtils.isBlank;


public class SecondFactorAuthenticator {

    private static final Logger logger = PayLoggerFactory.getLogger(SecondFactorAuthenticator.class);

    private static GoogleAuthenticator authenticator = new GoogleAuthenticator(getConfig());

    public static int newPassCode(String secret) {
        checkNull(secret);
        return authenticator.getTotpPassword(base32().encode(secret.getBytes()));
    }

    public static boolean authorize(String secret, int passcode) {
        checkNull(secret);
        return authenticator.authorize(base32().encode(secret.getBytes()), passcode);
    }

    private static void checkNull(String secret) {
        if(isBlank(secret)) {
            String error = "supplied a null/empty otpKey for second factor";
            logger.error(error);
            throw new RuntimeException(error);
        }
    }

    private static GoogleAuthenticatorConfig getConfig() {
        return new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setCodeDigits(6)
                //60 second validity period??
                .setTimeStepSizeInMillis(TimeUnit.SECONDS.toMillis(60))
                .build();
    }
}
