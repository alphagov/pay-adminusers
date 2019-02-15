package uk.gov.pay.adminusers.utils.telephonenumber;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TelephoneNumberUtilityFormatToE164InvalidDataTest {

    private String telephoneNumber;

    public TelephoneNumberUtilityFormatToE164InvalidDataTest(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    @Parameterized.Parameters
    public static Object[] data() {
        return new Object[]{
                null,
                "",
                " ",
                "abc",
                "(╯°□°）╯︵ ┻━┻"
        };
    }

    @Test(expected = RuntimeException.class)
    public void formatToE164_shouldThrowNumberParseExceptionOnInvalidTelephoneNumber() {
        TelephoneNumberUtility.formatToE164(telephoneNumber);
    }

}
