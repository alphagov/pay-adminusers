package uk.gov.pay.adminusers.utils.telephonenumber;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class TelephoneNumberUtilityFormatToE164ValidUnitedKingdomDataTest {

    private static final String TEST_RESULT = "+441134960000";

    private String telephoneNumber;

    public TelephoneNumberUtilityFormatToE164ValidUnitedKingdomDataTest(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    @Parameterized.Parameters
    public static Object[] data() {
        return new Object[]{
                // local format
                "01134960000",
                "0113 496 0000",
                "0113-496-0000",
                "(0113) 496 0000",
                "(0113) 496-0000",
                "(0113) / 496-0000",
                "   01134960000   ",
                // international format
                "+441134960000",
                "+44113 496 0000",
                "+44113-496-0000",
                "(+44113) 496 0000",
                "(+44113) 496-0000",
                "(+44113) / 496-0000",
                "   +441134960000   ",
                "00441134960000",
                "0044113 496 0000",
                "0044113-496-0000",
                "(0044113) 496 0000",
                "(0044113) 496-0000",
                "(0044113) / 496-0000",
                "   00441134960000   "
        };
    }

    @Test
    public void formatToE164_shouldEvaluateToE164FormattedTelephoneNumber() {
        String result = TelephoneNumberUtility.formatToE164(telephoneNumber);
        assertThat(result, is(TEST_RESULT));
    }

}
