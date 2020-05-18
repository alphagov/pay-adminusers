package uk.gov.pay.adminusers.utils.email;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;

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
        return List.of(new Object[][] {
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
                {"valid@accessplanit.com", true},
                {"valid@assembly.wales", true},
                {"valid@caa.co.uk", true},
                {"valid@careinspectorate.com", true},
                {"valid@cynulliad.cymru", true},
                {"valid@derrystrabane.com", true},
                {"valid@gov.scot", true},
                {"valid@gov.uk", true},
                {"valid@gov.wales", true},
                {"valid@hial.co.uk", true},
                {"valid@hmcts.net", true},
                {"valid@judiciary.uk", true},
                {"valid@llyw.cymru", true},
                {"valid@mil.uk", true},
                {"valid@mod.uk", true},
                {"valid@naturalengland.org.uk", true},
                {"valid@nhm.ac.uk", true},
                {"valid@nhs.net", true},
                {"valid@nhs.uk", true},
                {"valid@nmandd.org", true},
                {"valid@nmni.com", true},
                {"valid@ogauthority.co.uk", true},
                {"valid@parliament.scot", true},
                {"valid@parliament.uk", true},
                {"valid@police.uk", true},
                {"valid@prrt.org", true},
                {"valid@scotent.co.uk", true},
                {"valid@serc.ac.uk", true},
                {"valid@slc.co.uk", true},
                {"valid@socialworkengland.org.uk", true},
                {"valid@sssc.uk.com", true},
                {"valid@ucds.email", true},
                {"valid@wmca.org.uk", true},
                {"valid@york.ac.uk", true},

                // all valid emails with subdomains
                {"valid@subdomain.acas.org.uk", true},
                {"valid@subdomain.accessplanit.com", true},
                {"valid@subdomain.assembly.wales", true},
                {"valid@subdomain.careinspectorate.com", true},
                {"valid@subdomain.cynulliad.cymru", true},
                {"valid@subdomain.derrystrabane.com", true},
                {"valid@subdomain.gov.scot", true},
                {"valid@subdomain.gov.uk", true},
                {"valid@subdomain.gov.wales", true},
                {"valid@subdomain.hial.co.uk", true},
                {"valid@subdomain.hmcts.net", true},
                {"valid@subdomain.judiciary.uk", true},
                {"valid@subdomain.llyw.cymru", true},
                {"valid@subdomain.mil.uk", true},
                {"valid@subdomain.mod.uk", true},
                {"valid@subdomain.naturalengland.org.uk", true},
                {"valid@subdomain.nhm.ac.uk", true},
                {"valid@subdomain.nhs.net", true},
                {"valid@subdomain.nhs.uk", true},
                {"valid@subdomain.nmandd.org", true},
                {"valid@subdomain.nmni.com", true},
                {"valid@subdomain.ogauthority.co.uk", true},
                {"valid@subdomain.parliament.scot", true},
                {"valid@subdomain.parliament.uk", true},
                {"valid@subdomain.police.uk", true},
                {"valid@subdomain.prrt.org", true},
                {"valid@subdomain.scotent.co.uk", true},
                {"valid@subdomain.serc.ac.uk", true},
                {"valid@subdomain.slc.co.uk", true},
                {"valid@subdomain.socialworkengland.org.uk", true},
                {"valid@subdomain.ucds.email", true},
                {"valid@subdomain.sssc.uk.com", true},
                {"valid@subdomain.wmca.org.uk", true},
                {"valid@subdomain.york.ac.uk", true},

        });
    }

    @Test
    public void isPublicSectorEmail_shouldEvaluateWhetherOrNotItIsPublicSectorEmail() {
        boolean result = EmailValidator.isPublicSectorEmail(email);
        assertThat("Expected " + email + " to be " + (testResult ? "valid" : "invalid"), result, is(testResult));
    }
}
