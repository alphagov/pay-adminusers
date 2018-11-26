package uk.gov.pay.adminusers.utils.email;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class EmailValidatorIsPublicSectorEmailTest {

    private String email;

    private boolean testResult;

    public EmailValidatorIsPublicSectorEmailTest(String email, boolean testResult) {
        this.email = email;
        this.testResult = testResult;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // main validations
                {"", false},
                {"@", false},

                // gov.uk validations
                {".gov.uk", false},
                {"..gov.uk", false},
                {"gov.uk", false},
                {"@gov.uk", false},
                {"@@gov.uk", false},
                {"invalid@gov.uk.", false},
                {"invalid@-gov.uk", false},
                {"invalid@-subdomain.gov.uk", false},
                {"invalid@a+gov.uk", false},
                {"@gov.sub1.uk", false},
                {"valid@sub2.sub1.gov.uk", true},
                {"valid@sub2-2.sub2-1.sub1.gov.uk", true},

                // naturalengland.org.uk validations
                {".naturalengland.org.uk", false},
                {"..naturalengland.org.uk", false},
                {"naturalengland.org.uk", false},
                {"@naturalengland.org.uk", false},
                {"@@naturalengland.org.uk", false},
                {"invalid@naturalengland.org.uk.", false},
                {"invalid@-naturalengland.org.uk", false},
                {"invalid@-subdomain.naturalengland.org.uk", false},
                {"invalid@a+naturalengland.org.uk", false},
                {"@naturalengland.org.sub1.uk", false},
                {"valid@sub2.sub1.naturalengland.org.uk", true},
                {"valid@sub2-2.sub2-1.sub1.naturalengland.org.uk", true},

                // all valid emails with domains
                {"valid@acas.org.uk", true},
                {"valid@assembly.wales", true},
                {"valid@cynulliad.cymru", true},
                {"valid@gov.scot", true},
                {"valid@gov.uk", true},
                {"valid@gov.wales", true},
                {"valid@hmcts.net", true},
                {"valid@judiciary.uk", true},
                {"valid@llyw.cymru", true},
                {"valid@mil.uk", true},
                {"valid@mod.uk", true},
                {"valid@naturalengland.org.uk", true},
                {"valid@nhm.ac.uk", true},
                {"valid@nhs.net", true},
                {"valid@nhs.uk", true},
                {"valid@parliament.scot", true},
                {"valid@parliament.uk", true},
                {"valid@police.uk", true},
                {"valid@scotent.co.uk", true},
                {"valid@slc.co.uk", true},
                {"valid@ucds.email", true},


                // all valid emails with subdomains
                {"valid@subdomain.acas.org.uk", true},
                {"valid@subdomain.assembly.wales", true},
                {"valid@subdomain.cynulliad.cymru", true},
                {"valid@subdomain.gov.scot", true},
                {"valid@subdomain.gov.uk", true},
                {"valid@subdomain.gov.wales", true},
                {"valid@subdomain.hmcts.net", true},
                {"valid@subdomain.judiciary.uk", true},
                {"valid@subdomain.llyw.cymru", true},
                {"valid@subdomain.mil.uk", true},
                {"valid@subdomain.mod.uk", true},
                {"valid@subdomain.naturalengland.org.uk", true},
                {"valid@subdomain.nhm.ac.uk", true},
                {"valid@subdomain.nhs.net", true},
                {"valid@subdomain.nhs.uk", true},
                {"valid@subdomain.parliament.scot", true},
                {"valid@subdomain.parliament.uk", true},
                {"valid@subdomain.police.uk", true},
                {"valid@subdomain.scotent.co.uk", true},
                {"valid@subdomain.slc.co.uk", true},
                {"valid@subdomain.ucds.email", true}

        });
    }

    @Test
    public void isPublicSectorEmail_shouldEvaluateWhetherOrNotItIsPublicSectorEmail() {
        boolean result = EmailValidator.isPublicSectorEmail(email);
        assertThat("Expected " + email + " to be " + (testResult ? "valid" : "invalid"), result, is(testResult));
    }
}
