package uk.gov.pay.adminusers.utils;

import java.util.Comparator;

public class Comparators {

    public static Comparator<String> usingNumericComparator() {
        return Comparator.comparingLong(Long::valueOf);
    }
}
