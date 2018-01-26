package uk.gov.pay.adminusers.utils;

import org.apache.commons.lang3.math.NumberUtils;

import java.util.Comparator;

public class Comparators {

    public static Comparator<String> usingNumericComparator() {
        return Comparator.comparingLong(Long::valueOf);
    }

    public static Comparator<String> compareGatewayAccounts() {
        return (o1, o2) -> {
            if (NumberUtils.isDigits(o1) && NumberUtils.isDigits(o2)){
                return Comparators.usingNumericComparator().compare(o1, o2);
            } else if (NumberUtils.isDigits(o1)) {
                return -1;
            } else if (NumberUtils.isDigits(o2)) {
                return 1;
            } else {
                return o1.compareTo(o2);
            }
        };
    }
}
