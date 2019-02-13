package uk.gov.pay.adminusers.utils.telephonenumber;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class TelephoneNumberUtilityIsValidPhoneNumberTest {

    private String telephoneNumber;

    private boolean testResult;

    public TelephoneNumberUtilityIsValidPhoneNumberTest(String telephoneNumber, boolean testResult) {
        this.telephoneNumber = telephoneNumber;
        this.testResult = testResult;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // valid phone numbers: local format
                {"01134960000", true},
                {"0113 496 0000", true},
                {"0113-496-0000", true},
                {"(0113) 496 0000", true},
                {"(0113) 496-0000", true},
                {"(0113) / 496-0000", true},
                {"   01134960000   ", true},

                // valid phone numbers: international format
                {"+441134960000", true},
                {"+44113 496 0000", true},
                {"+44113-496-0000", true},
                {"(+44113) 496 0000", true},
                {"(+44113) 496-0000", true},
                {"(+44113) / 496-0000", true},
                {"   +441134960000   ", true},
                {"00441134960000", true},
                {"0044113 496 0000", true},
                {"0044113-496-0000", true},
                {"(0044113) 496 0000", true},
                {"(0044113) 496-0000", true},
                {"(0044113) / 496-0000", true},
                {"   00441134960000   ", true},

                // invalid phone numbers
                {null, false},
                {"", false},
                {"  ", false},
                {"07700900000", false}, // example phone number (valid format, but invalid as a real phone number)
                {"0770 090 0000", false},
                {"0770-090-0000", false},
                {"(0770) 090 0000", false},
                {"(0770) 090-0000", false},
                {"(0770) / 090-0000", false},
                {"   07700900000   ", false},
                {"0113496443a", false},
                {"+44113496443a", false},
                {"0044113496443a", false},
                {"0770090000a", false},
                {"O2O793O0000", false}, // letter "O" instead of "0"
                {"abc", false}
        });
    }

    @Test
    public void isValidPhoneNumber_shouldEvaluateWhetherOrNotItIsValidPhoneNumber() {
        boolean result = TelephoneNumberUtility.isValidPhoneNumber(telephoneNumber);
        assertThat("Expected " + telephoneNumber + " to be " + (testResult ? "valid" : "invalid"), result, is(testResult));
    }

}
