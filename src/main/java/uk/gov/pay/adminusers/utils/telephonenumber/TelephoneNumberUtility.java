package uk.gov.pay.adminusers.utils.telephonenumber;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import org.apache.commons.lang3.StringUtils;

public class TelephoneNumberUtility {

    private static final String DEFAULT_COUNTRY = "GB";

    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    public static boolean isValidPhoneNumber(String telephoneNumber) {
        try {
            if (StringUtils.isNotBlank(telephoneNumber)) {
                PhoneNumber phoneNumber = PHONE_NUMBER_UTIL.parseAndKeepRawInput(telephoneNumber, DEFAULT_COUNTRY);
                return PHONE_NUMBER_UTIL.isValidNumber(phoneNumber);
            }
        } catch (NumberParseException e) {
            // do nothing
        }
        return false;
    }

    public static String formatToE164(String telephoneNumber) {
        try {
            PhoneNumber phoneNumber = PHONE_NUMBER_UTIL.parseAndKeepRawInput(telephoneNumber, DEFAULT_COUNTRY);
            return PHONE_NUMBER_UTIL.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            // we wrap the checked NumberParseException exception
            // because we use the method in lambdas
            throw new RuntimeException(e);
        }
    }

}
