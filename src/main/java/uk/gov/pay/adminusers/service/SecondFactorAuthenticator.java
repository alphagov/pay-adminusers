package uk.gov.pay.adminusers.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.slf4j.Logger;
import uk.gov.pay.adminusers.logger.PayLoggerFactory;

import static com.google.common.io.BaseEncoding.base32;
import static org.apache.commons.lang3.StringUtils.isBlank;


public class SecondFactorAuthenticator {

    private static final Logger logger = PayLoggerFactory.getLogger(SecondFactorAuthenticator.class);

    private final GoogleAuthenticator authenticator;

    public SecondFactorAuthenticator() {
        this.authenticator = new GoogleAuthenticator();
    }

    public int newPassCode(String secret) {
        checkNull(secret);
        return authenticator.getTotpPassword(base32().encode(secret.getBytes()));
    }

    public boolean authorize(String secret, int passcode) {
        checkNull(secret);
        return authenticator.authorize(base32().encode(secret.getBytes()), passcode);
    }

    private void checkNull(String secret) {
        if (isBlank(secret)) {
            String error = "supplied a null/empty otpKey for second factor";
            logger.error(error);
            throw new RuntimeException(error);
        }
    }

}
