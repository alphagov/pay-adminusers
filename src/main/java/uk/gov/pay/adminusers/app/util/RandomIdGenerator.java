package uk.gov.pay.adminusers.app.util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

public class RandomIdGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Random RANDOM = new Random();

    /**
     * This method will generate a URL safe random string.
     * This works by choosing 130 bits from a cryptographically secure random bit generator,
     * and encoding them in base-32.
     * <p> 128 bits is considered to be cryptographically strong,
     * but each digit in a base 32 number can encode 5 bits, so 128 is rounded up to the next multiple of 5.
     * This encoding is compact and efficient, with 5 random bits per character. Compare this to a random UUID,
     * which only has 3.4 bits per character in standard layout, and only 122 random bits in total </p>
     *
     * @return a random number in base32 (in string format)
     */
    public static String newId() {
        return new BigInteger(130, SECURE_RANDOM).toString(32);
    }

    public static Long newLongId() {
        return (long) Math.floor(Math.random() * 9000000000L) + 1000000000L;
    }

    public static Integer randomInt(){
        return RANDOM.nextInt(Integer.MAX_VALUE);
    }
}
