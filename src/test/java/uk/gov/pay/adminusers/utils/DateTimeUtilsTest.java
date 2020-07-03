package uk.gov.pay.adminusers.utils;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.pay.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;

public class DateTimeUtilsTest {

    @Test
    public void formatsAsIsoInstantWithMillisecondPrecision() {
        ZonedDateTime timestamp = ZonedDateTime.parse("2010-12-31T22:59:59.132012345Z");
        final String actual = ISO_INSTANT_MILLISECOND_PRECISION.format(timestamp);
        final String expected = "2010-12-31T22:59:59.132Z";
        assertEquals(expected, actual);
    }
}
