package uk.gov.pay.adminusers.utils.telephonenumber;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TelephoneNumberUtilityFormatToE164ValidUnitedKingdomDataTest {

    private static final String TEST_RESULT = "+441134960000";

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

    @ParameterizedTest
    @MethodSource("data")
    public void formatToE164_shouldEvaluateToE164FormattedTelephoneNumber(String telephoneNumber) {
        String result = TelephoneNumberUtility.formatToE164(telephoneNumber);
        assertThat(result, is(TEST_RESULT));
    }
}
