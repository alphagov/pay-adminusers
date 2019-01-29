package uk.gov.pay.adminusers.utils;

import org.junit.Test;

import java.time.ZonedDateTime;

import static junit.framework.TestCase.assertEquals;


public class DateTimeUtilsTest {

    @Test
    public void toUTCDateString() {
        ZonedDateTime timestamp = ZonedDateTime.parse("2010-12-31T22:59:59.132012345Z");
        final String actual = DateTimeUtils.toUTCDateString(timestamp);
        final String expected = "2010-12-31T22:59:59.132Z";
        assertEquals(expected, actual);
    }
}
