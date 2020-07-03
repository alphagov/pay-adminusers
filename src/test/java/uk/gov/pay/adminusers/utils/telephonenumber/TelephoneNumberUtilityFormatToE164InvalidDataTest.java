package uk.gov.pay.adminusers.utils.telephonenumber;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TelephoneNumberUtilityFormatToE164InvalidDataTest {

    public static Object[] data() {
        return new Object[]{
                null,
                "",
                " ",
                "abc",
                "(╯°□°）╯︵ ┻━┻"
        };
    }

    @ParameterizedTest
    @MethodSource("data")
    public void formatToE164_shouldThrowNumberParseExceptionOnInvalidTelephoneNumber(String telephoneNumber) {
        assertThrows(RuntimeException.class, 
                () -> TelephoneNumberUtility.formatToE164(telephoneNumber));
    }
}
