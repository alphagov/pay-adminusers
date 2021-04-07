package uk.gov.pay.adminusers.utils.email;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

public class EmailValidator {

    /**
     * {@link #PUBLIC_SECTOR_EMAIL_DOMAIN_REGEX_PATTERNS_IN_ASCENDING_ORDER} is based on:<br>
     * - <a href="https://en.wikipedia.org/wiki/.uk">en.wikipedia.org/wiki/.uk</a><br>
     * - <a href="https://github.com/alphagov/notifications-admin/blob/9391181b2c7d077ea8fe0a72c718ab8f7fdbcd0c/app/config.py#L67">alphagov/notifications-admin</a><br>
     */
    private static final List<String> PUBLIC_SECTOR_EMAIL_DOMAIN_REGEX_PATTERNS_IN_ASCENDING_ORDER = List.of(
            "acas.org.uk",
            "accessplanit.com",
            "assembly.wales",
            "bl.uk",
            "caa.co.uk",
            "careinspectorate.com",
            "cynulliad.cymru",
            "derrystrabane.com",
            "eani.org.uk",
            "fermanaghomagh.com",
            "gov.scot",
            "gov.uk",
            "gov.wales",
            "hial.co.uk",
            "hmcts.net",
            "judiciary.uk",
            "llyw.cymru",
            "mil.uk",
            "mod.uk",
            "naturalengland.org.uk",
            "nature.scot",
            "nhm.ac.uk",
            "nhs.net",
            "nhs.scot",
            "nhs.uk",
            "nls.uk",
            "nmandd.org",
            "nmni.com",
            "ogauthority.co.uk",
            "os.uk",
            "parliament.scot",
            "parliament.uk",
            "police.uk",
            "prrt.org",
            "scotent.co.uk",
            "serc.ac.uk",
            "slc.co.uk",
            "socialworkengland.org.uk",
            "sssc.uk.com",
            "ucds.email",
            "uksbs.co.uk",
            "wmca.org.uk",
            "york.ac.uk",
            "digitalaccessibilitycentre.org"
    );

    private static final Pattern PUBLIC_SECTOR_EMAIL_DOMAIN_REGEX_PATTERN;
    static {
        String domainRegExPatternString = PUBLIC_SECTOR_EMAIL_DOMAIN_REGEX_PATTERNS_IN_ASCENDING_ORDER
                .stream()
                .map(Pattern::quote)
                .collect(joining("|"));

        // We are splitting the logic into two parts for whitelisted domains and subdomains
        String regExDomainsOnlyPart = "(" + domainRegExPatternString + ")";
        String regExSubdomainsPart = "(((?!-)[A-Za-z0-9-]+(?<!-)\\.)+(" + domainRegExPatternString + "))";
        PUBLIC_SECTOR_EMAIL_DOMAIN_REGEX_PATTERN =
                Pattern.compile("^" + regExDomainsOnlyPart +  "|" + regExSubdomainsPart + "$");
    }

    private static final org.apache.commons.validator.routines.EmailValidator COMMONS_EMAIL_VALIDATOR =
            org.apache.commons.validator.routines.EmailValidator.getInstance();

    public static boolean isValid(String email) {
        return COMMONS_EMAIL_VALIDATOR.isValid(email);
    }

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
