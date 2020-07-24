package uk.gov.pay.adminusers.utils.email;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailValidator {

    /**
     * {@link #PUBLIC_SECTOR_EMAIL_DOMAIN_REGEX_PATTERNS_IN_ASCENDING_ORDER} is based on:<br>
     * - <a href="https://en.wikipedia.org/wiki/.uk">en.wikipedia.org/wiki/.uk</a><br>
     * - <a href="https://github.com/alphagov/notifications-admin/blob/9391181b2c7d077ea8fe0a72c718ab8f7fdbcd0c/app/config.py#L67">alphagov/notifications-admin</a><br>
     */
    private static final List<String> PUBLIC_SECTOR_EMAIL_DOMAIN_REGEX_PATTERNS_IN_ASCENDING_ORDER = List.of(
            "acas\\.org\\.uk",
            "accessplanit\\.com",
            "assembly\\.wales",
            "bl\\.uk",
            "caa\\.co\\.uk",
            "careinspectorate\\.com",
            "cynulliad\\.cymru",
            "derrystrabane\\.com",
            "gov\\.scot",
            "gov\\.uk",
            "gov\\.wales",
            "hial\\.co\\.uk",
            "hmcts\\.net",
            "judiciary\\.uk",
            "llyw\\.cymru",
            "mil\\.uk",
            "mod\\.uk",
            "naturalengland\\.org\\.uk",
            "nature\\.scot",
            "nhm\\.ac\\.uk",
            "nhs\\.net",
            "nhs\\.uk",
            "nmandd\\.org",
            "nmni\\.com",
            "ogauthority\\.co.uk",
            "parliament\\.scot",
            "parliament\\.uk",
            "police\\.uk",
            "prrt\\.org",
            "scotent\\.co\\.uk",
            "serc\\.ac\\.uk", 
            "slc\\.co\\.uk",
            "socialworkengland\\.org\\.uk",
            "sssc\\.uk\\.com",
            "ucds\\.email",
            "wmca\\.org\\.uk",
            "york\\.ac\\.uk",
            "digitalaccessibilitycentre\\.org"
    );

    private static final Pattern PUBLIC_SECTOR_EMAIL_DOMAIN_REGEX_PATTERN;
    static {
        String domainRegExPatternString = String.join("|", PUBLIC_SECTOR_EMAIL_DOMAIN_REGEX_PATTERNS_IN_ASCENDING_ORDER);

        // We are splitting the logic into two parts for whitelisted domains and subdomains
        String regExDomainsOnlyPart = "(" + domainRegExPatternString + ")";
        String regExSubdomainsPart = "(((?!-)[A-Za-z0-9-]+(?<!-)\\.)+(" + domainRegExPatternString + "))";
        PUBLIC_SECTOR_EMAIL_DOMAIN_REGEX_PATTERN =
                Pattern.compile("^" + regExDomainsOnlyPart +  "|" + regExSubdomainsPart + "$");
    }

    private static final org.apache.commons.validator.routines.EmailValidator COMMONS_EMAIL_VALIDATOR =
            org.apache.commons.validator.routines.EmailValidator.getInstance();

    /**
     * <p>Checks if a field has a valid e-mail address.</p>
     *
     * @param email The value validation is being performed on.  A <code>null</code>
     *              value is considered invalid.
     * @return true if the email address is valid.
     */
    public static boolean isValid(String email) {
        return COMMONS_EMAIL_VALIDATOR.isValid(email);
    }

    /**
     * This method checks that an email belongs to a public sector domain name
     * <p>
     * The logic is split into two parts:
     * <ol>
     *   <li>checks the subdomains and that they:</li>
     *   <ul>
     *     <li>don't start with "-" </li>
     *     <li>only have any alphanumeric characters or "-"</li>
     *     <li>don't end with "-"</li>
     *   </ul>
     *   <li>checks the whitelisted domains</li>
     * </ol>
     * </p>
     *
     * @param email the email to check
     * @return <b>boolean</b> whether or not it is from a public sector domain
     */
    public static boolean isPublicSectorEmail(String email) {
        String lowerCaseEmail = email.toLowerCase(Locale.ENGLISH);
        String[] emailParts = lowerCaseEmail.split("@");
        if (emailParts.length != 2 || emailParts[0].isEmpty() || emailParts[1].isEmpty()) {
            return false;
        }
        String domain = emailParts[1];

        Matcher matcher = PUBLIC_SECTOR_EMAIL_DOMAIN_REGEX_PATTERN.matcher(domain);

        return matcher.matches();
    }
}
