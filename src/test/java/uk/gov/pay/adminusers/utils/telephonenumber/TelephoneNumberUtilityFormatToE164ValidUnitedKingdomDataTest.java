package uk.gov.pay.adminusers.utils.telephonenumber;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class TelephoneNumberUtilityFormatToE164ValidUnitedKingdomDataTest {

    private static final String TEST_RESULT = "+442079304433";

    private String telephoneNumber;

    public TelephoneNumberUtilityFormatToE164ValidUnitedKingdomDataTest(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    @Parameterized.Parameters
    public static Object[] data() {
        return new Object[]{
                // local format
                "02079304433",
                "0207 930 4433",
                "0207-930-4433",
                "(0207) 930 4433",
                "(0207) 930-4433",
                "(0207) / 930-4433",
                "   02079304433   ",
                // international format
                "+442079304433",
                "+44207 930 4433",
                "+44207-930-4433",
                "(+44207) 930 4433",
                "(+44207) 930-4433",
                "(+44207) / 930-4433",
                "   +442079304433   ",
                "00442079304433",
                "0044207 930 4433",
                "0044207-930-4433",
                "(0044207) 930 4433",
                "(0044207) 930-4433",
                "(0044207) / 930-4433",
                "   00442079304433   "
        };
    }

    @Test
    public void formatToE164_shouldEvaluateToE164FormattedTelephoneNumber() {
        String result = TelephoneNumberUtility.formatToE164(telephoneNumber);
        assertThat("Expected " + telephoneNumber + " to be " + TEST_RESULT, result, is(TEST_RESULT));
    }

}
