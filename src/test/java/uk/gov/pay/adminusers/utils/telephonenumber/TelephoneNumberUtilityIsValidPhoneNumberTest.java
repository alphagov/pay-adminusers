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
                {"02079304433", true},
                {"0207 930 4433", true},
                {"0207-930-4433", true},
                {"(0207) 930 4433", true},
                {"(0207) 930-4433", true},
                {"(0207) / 930-4433", true},
                {"   02079304433   ", true},

                // valid phone numbers: international format
                {"+442079304433", true},
                {"+44207 930 4433", true},
                {"+44207-930-4433", true},
                {"(+44207) 930 4433", true},
                {"(+44207) 930-4433", true},
                {"(+44207) / 930-4433", true},
                {"   +442079304433   ", true},
                {"00442079304433", true},
                {"0044207 930 4433", true},
                {"0044207-930-4433", true},
                {"(0044207) 930 4433", true},
                {"(0044207) 930-4433", true},
                {"(0044207) / 930-4433", true},
                {"   00442079304433   ", true},

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
                {"0207930443a", false},
                {"+44207930443a", false},
                {"0044207930443a", false},
                {"0770090000a", false},
                {"O2O793O4433", false}, // letter "O" instead of "0"
                {"abc", false}
        });
    }

    @Test
    public void isValidPhoneNumber_shouldEvaluateWhetherOrNotItIsValidPhoneNumber() {
        boolean result = TelephoneNumberUtility.isValidPhoneNumber(telephoneNumber);
        assertThat("Expected " + telephoneNumber + " to be " + (testResult ? "valid" : "invalid"), result, is(testResult));
    }

}
